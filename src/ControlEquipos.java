
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.JOptionPane;

public class ControlEquipos extends javax.swing.JFrame {

    // Carpeta en el proyecto donde guardaremos los txt
    private final File carpeta = new File("archivos");

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ControlEquipos.class.getName());

    /**
     * Constructor
     */
    public ControlEquipos() {
        initComponents(); 
        this.setLocationRelativeTo(null); 
    }

    // Obtener "revisiones.txt" creando la carpeta si no existe
    private File obtenerArchivo() throws Exception {
        if (!carpeta.exists()) {
            Files.createDirectories(carpeta.toPath());
        }
        return new File(carpeta, "revisiones.txt");
    }

    // Muestra errores
    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Ocurrió un problema :C", JOptionPane.ERROR_MESSAGE);
    }


    // 1. Registrar equipo: agrega al final del archivo sin borrar anteriores
    private void registrarEquipo() {
        try {
            String codigo = txtCodigo.getText().trim();

            if (codigo.length() != 5) {
                throw new Exception("El código debe tener exactamente 5 caracteres (ejemplo: PC001).");
            }

            File archivo = obtenerArchivo();
            int siguienteFolio = obtenerSiguienteFolio(archivo);

            // Formato de folio a 4 dígitos (ejemplo: 0001)
            String folioStr = String.format("%04d", siguienteFolio);

            // Registro exacto de 13 bytes: 4 (folio) + 1 (espacio) + 5 (código) + 1 (espacio) + 1 (P) + 1 (\n)
            String nuevoRegistro = folioStr + " " + codigo + " P\n";

            // Pasamos 'true' en FileOutputStream para activar el modo append
            try (BufferedWriter escritor = new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(archivo, true), StandardCharsets.UTF_8))) {
                escritor.write(nuevoRegistro);
            }

            JOptionPane.showMessageDialog(this, "Equipo registrado con el folio: " + folioStr);
            txtCodigo.setText("");
            txtFolio.setText(folioStr);
            mostrarRegistros();

        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // Calcula el folio contando las líneas actuales
    private int obtenerSiguienteFolio(File archivo) {
        if (!archivo.exists()) {
            return 1;
        }
        int totalLineas = 0;
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            while (lector.readLine() != null) {
                totalLineas++;
            }
        } catch (Exception e) {
            return 1;
        }
        return totalLineas + 1;
    }

    // 2. Mostrar registros: lectura secuencial línea por línea
    private void mostrarRegistros() {
        try {
            File archivo = obtenerArchivo();
            if (!archivo.exists()) {
                txtRegistros.setText("El archivo revisiones.txt no existe aún.");
                return;
            }

            StringBuilder contenido = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    contenido.append(linea).append("\n");
                }
            }

            txtRegistros.setText(contenido.toString());
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // 3. Buscar folio: lectura secuencial evaluando el inicio de línea
    private void buscarFolio() {
        try {
            String folioBuscado = txtFolio.getText().trim();
            if (folioBuscado.isEmpty()) {
                throw new Exception("Escribe un folio para buscar.");
            }

            File archivo = obtenerArchivo();
            if (!archivo.exists()) {
                throw new Exception("El archivo no existe.");
            }

            boolean encontrado = false;
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.startsWith(folioBuscado)) {
                        JOptionPane.showMessageDialog(this, "Registro encontrado!:\n" + linea);
                        encontrado = true;
                        break;
                    }
                }
            }

            if (!encontrado) {
                JOptionPane.showMessageDialog(this, "El folio " + folioBuscado + " no fue encontrado :c.");
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // 4. Marcar como revisado: RandomAccessFile y seek()
    private void marcarComoRevisado() {
        try {
            int folio = Integer.parseInt(txtFolio.getText().trim());
            File archivo = obtenerArchivo();

            if (!archivo.exists()) {
                throw new Exception("El archivo revisiones.txt no existe.");
            }

            // Fórmula indicada:
            long posicionEstado = (folio - 1) * 13L + 11L;

            if (posicionEstado < 0 || posicionEstado >= archivo.length()) {
                throw new Exception("El folio ingresado no existe en el archivo :c.");
            }

            // "rw" abre el archivo para lectura y escritura directa
            try (RandomAccessFile raf = new RandomAccessFile(archivo, "rw")) {
                raf.seek(posicionEstado); // Nos posicionamos directamente en el byte del estado
                raf.write('R');           // Sobrescribimos 'P' con 'R'
            }

            JOptionPane.showMessageDialog(this, "Equipo con folio " + folio + " actualizado a Revisado (R).");
            mostrarRegistros();

        } catch (NumberFormatException ex) {
            mostrarError(new Exception("Ingresa un número de folio válido por favor."));
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // 5. Crear copia usando flujos de bytes
    private void crearCopia() {
        try {
            File origen = obtenerArchivo();
            if (!origen.exists()) {
                throw new Exception("Primero debes registrar equipos o crear el archivo.");
            }

            File destino = new File(carpeta, "revisiones_copia.txt");

            try (BufferedInputStream entrada = new BufferedInputStream(new FileInputStream(origen));
                 BufferedOutputStream salida = new BufferedOutputStream(new FileOutputStream(destino))) {

                byte[] buffer = new byte[1024];
                int bytesLeidos;
                while ((bytesLeidos = entrada.read(buffer)) != -1) {
                    salida.write(buffer, 0, bytesLeidos);
                }
            }

            JOptionPane.showMessageDialog(this, "Copia creada con éxito :D : " + destino.getName());
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // 6. Mover archivo 
    private void moverArchivo() {
        try {
            File origen = obtenerArchivo();
            if (!origen.exists()) {
                throw new Exception("El archivo revisiones.txt no existe :c.");
            }

            File destino = new File(carpeta, "revisiones_movido.txt");

            Files.move(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JOptionPane.showMessageDialog(this, "Archivo movido/renombrado a: " + destino.getName());
            mostrarRegistros();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // 7. Eliminar archivo con confirmación 
    private void eliminarArchivo() {
        try {
            File archivo = obtenerArchivo();
            if (!archivo.exists()) {
                throw new Exception("El archivo revisiones.txt no existe.");
            }

            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas eliminar el archivo " + archivo.getName() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                Files.delete(archivo.toPath());
                txtRegistros.setText("");
                JOptionPane.showMessageDialog(this, "Archivo eliminado correctamente :o .");
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    // Contar pendientes y revisados
    private void contarEstados() {
        try {
            File archivo = obtenerArchivo();
            if (!archivo.exists()) {
                throw new Exception("El archivo no existe.");
            }

            int pendientes = 0;
            int revisados = 0;

            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.endsWith("P")) {
                        pendientes++;
                    } else if (linea.endsWith("R")) {
                        revisados++;
                    }
                }
            }

            JOptionPane.showMessageDialog(this,
                "--- Conteo de Equipos ---\n" +
                "• Pendientes (P): " + pendientes + "\n" +
                "• Revisados (R): " + revisados
            );
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtFolio = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtRegistros = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        btnRegistrar = new javax.swing.JButton();
        btnMostrar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        btnMarcarRevisado = new javax.swing.JButton();
        btnCopiar = new javax.swing.JButton();
        btnMover = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnContar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Código del equipo:");

        jLabel2.setText("Folio:");

        txtRegistros.setColumns(20);
        txtRegistros.setRows(5);
        jScrollPane1.setViewportView(txtRegistros);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("ControlRevisionEquipos");

        btnRegistrar.setText("<html><center>Registrar<br>equipo</center></html>");
        btnRegistrar.addActionListener(this::btnRegistrarActionPerformed);

        btnMostrar.setText("<html><center>Mostrar<br>registros</center></html>");
        btnMostrar.addActionListener(this::btnMostrarActionPerformed);

        btnBuscar.setText("<html><center>Buscar<br>folio</center></html>");
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);

        btnMarcarRevisado.setText("<html><center>Marcar<br><center>como<br>revisado</center></html>");
        btnMarcarRevisado.addActionListener(this::btnMarcarRevisadoActionPerformed);

        btnCopiar.setText("<html><center>Crear<br>copia</center></html>");
        btnCopiar.addActionListener(this::btnCopiarActionPerformed);

        btnMover.setText("<html><center>Mover<br>archivo</center></html>");
        btnMover.addActionListener(this::btnMoverActionPerformed);

        btnEliminar.setText("<html><center>Eliminar<br>archivo</center></html>");
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);

        btnContar.setText("<html><center>Contar<br>estados</center></html>");
        btnContar.addActionListener(this::btnContarActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnRegistrar, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnMostrar, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnMarcarRevisado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(16, 16, 16)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtFolio, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(111, 111, 111)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 34, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(btnCopiar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnMover, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnContar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtFolio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRegistrar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMostrar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMarcarRevisado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCopiar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMover, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnContar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCopiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopiarActionPerformed
        crearCopia();
    }//GEN-LAST:event_btnCopiarActionPerformed

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarActionPerformed
        registrarEquipo();
    }//GEN-LAST:event_btnRegistrarActionPerformed

    private void btnMostrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMostrarActionPerformed
        mostrarRegistros();
    }//GEN-LAST:event_btnMostrarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        buscarFolio();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnMarcarRevisadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarcarRevisadoActionPerformed
        marcarComoRevisado();
    }//GEN-LAST:event_btnMarcarRevisadoActionPerformed

    private void btnMoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMoverActionPerformed
        moverArchivo();
    }//GEN-LAST:event_btnMoverActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarArchivo();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnContarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnContarActionPerformed
        contarEstados();
    }//GEN-LAST:event_btnContarActionPerformed


    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ControlEquipos().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnContar;
    private javax.swing.JButton btnCopiar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnMarcarRevisado;
    private javax.swing.JButton btnMostrar;
    private javax.swing.JButton btnMover;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField txtCodigo;
    private javax.swing.JTextField txtFolio;
    private javax.swing.JTextArea txtRegistros;
    // End of variables declaration//GEN-END:variables
}
