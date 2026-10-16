package org.example;
import org.iesalandalus.programacion.utilidades.Entrada;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class GestionDepartamentos {

    private static final String FILENAME = "AleatorioDepart.dat";
    private static final int RECORD_SIZE = 64;
    private static boolean modoDiagnostico = false;

    private enum EstadoPosicion {
        ACTIVA, ELIMINADA, NUNCA_UTILIZADA, ANOMALA
    }

    public static void main(String[] args) {
        System.out.println("Iniciando aplicación Gestión de Departamentos...");

        File file = new File(FILENAME);
        if (file.exists()) {
            System.out.println("El fichero ya existe. Comprobando integridad...");
            if (!integridad(true)) {
                System.out.println("\n¡ADVERTENCIA! Se han detectado anomalías de integridad en el fichero.");
                System.out.println("La aplicación entra en MODO DE DIAGNÓSTICO. Solo se permite inspección y lectura.\n");
                modoDiagnostico = true;
            } else {
                System.out.println("Integridad del fichero verificada correctamente.\n");
            }
        } else {
            try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw")) {
                System.out.println("Fichero '" + FILENAME + "' no existía y ha sido creado vacío.\n");
            } catch (IOException e) {
                System.err.println("Error fatal al crear el fichero inicial: " + e.getMessage());
                return;
            }
        }

        int opcion;
        do {
            System.out.println("=== MENÚ DE DEPARTAMENTOS ===");
            if (modoDiagnostico) System.out.println("[MODO DIAGNÓSTICO ACTIVO - EDICIÓN BLOQUEADA]");
            System.out.println("1. Comprobar si existe departamento");
            System.out.println("2. Consultar departamento");
            System.out.println("3. Insertar departamento");
            System.out.println("4. Actualizar número de empleados");
            System.out.println("5. Actualizar nombre y localidad");
            System.out.println("6. Eliminar departamento");
            System.out.println("7. Listado de departamentos activos");
            System.out.println("8. Inspección física");
            System.out.println("9. Estadísticas");
            System.out.println("10. Informe de integridad");
            System.out.println("0. Salir");

            opcion = leerEntero("Seleccione una opción: ", 0, 10);

            if (modoDiagnostico && opcion >= 3 && opcion <= 6) {
                System.out.println("\n-> OPERACIÓN DENEGADA por seguridad (Modo diagnóstico).\n");
                continue;
            }

            System.out.println();
            switch (opcion) {
                case 1:
                    existeDepartamento(leerEntero("ID Departamento a comprobar (1-32767): ", 1, 32767));
                    break;
                case 2:
                    consultaDepartamento(leerEntero("ID Departamento a consultar (1-32767): ", 1, 32767));
                    break;
                case 3:
                    int depIns = leerEntero("Número de departamento (1-32767): ", 1, 32767);
                    String nomIns = solicitarTexto("Nombre (máx 15 chars): ");
                    String locIns = solicitarTexto("Localidad (máx 15 chars): ");
                    short empIns = (short) leerEntero("Empleados (0-32767): ", 0, 32767);
                    insertaDepartamento(depIns, nomIns, locIns, empIns);
                    break;
                case 4:
                    int depEmp = leerEntero("Número de departamento: ", 1, 32767);
                    short nuevoEmp = (short) leerEntero("Nuevos empleados: ", 0, 32767);
                    actualizaNumeroEmpleados(depEmp, nuevoEmp);
                    break;
                case 5:
                    int depNomLoc = leerEntero("Número de departamento: ", 1, 32767);
                    String nuevoNom = solicitarTexto("Nuevo nombre: ");
                    String nuevaLoc = solicitarTexto("Nueva localidad: ");
                    actualizaNombreLocalidad(depNomLoc, nuevoNom, nuevaLoc);
                    break;
                case 6:
                    eliminaDepartamento(leerEntero("Número de departamento a eliminar: ", 1, 32767));
                    break;
                case 7:
                    listadoDepartamentos();
                    break;
                case 8:
                    inspeccionFisica();
                    break;
                case 9:
                    estadisticas();
                    break;
                case 10:
                    integridad(false);
                    break;
                case 0:
                    System.out.println("Finalizando aplicación...");
                    break;
            }
            System.out.println();
        } while (opcion != 0);
    }

    public static boolean existeDepartamento(int dep) {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long pos = (dep - 1) * (long) RECORD_SIZE;
            if (pos >= raf.length()) {
                System.out.println("Resultado: Posición situada fuera del fichero.");
                return false;
            }

            raf.seek(pos);
            short id = raf.readShort();
            String nom = leerString(raf, 15);
            String loc = leerString(raf, 15);
            short emp = raf.readShort();

            EstadoPosicion estado = determinarEstado(dep, id, nom, loc, emp);
            switch (estado) {
                case ACTIVA:
                    System.out.println("Resultado: Registro activo.");
                    return true;
                case ELIMINADA:
                    System.out.println("Resultado: Registro eliminado.");
                    return false;
                case NUNCA_UTILIZADA:
                    System.out.println("Resultado: Posición nunca utilizada.");
                    return false;
                case ANOMALA:
                    System.out.println("Resultado: Validación incorrecta (registro anómalo).");
                    return false;
            }
        } catch (IOException e) {
            System.out.println("Error de acceso de lectura: " + e.getMessage());
        }
        return false;
    }

    public static void consultaDepartamento(int dep) {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long pos = (dep - 1) * (long) RECORD_SIZE;
            if (pos >= raf.length()) {
                System.out.println("El departamento " + dep + " no existe (fuera del fichero).");
                return;
            }

            raf.seek(pos);
            short id = raf.readShort();
            String nom = leerString(raf, 15);
            String loc = leerString(raf, 15);
            short emp = raf.readShort();

            if (determinarEstado(dep, id, nom, loc, emp) == EstadoPosicion.ACTIVA) {
                System.out.printf("--- DEPARTAMENTO %d ---\n", id);
                System.out.println("Nombre:    " + nom.trim()); // Retirada de relleno al mostrar
                System.out.println("Localidad: " + loc.trim());
                System.out.println("Empleados: " + emp);
            } else {
                System.out.println("El departamento " + dep + " no existe o no está activo.");
            }
        } catch (IOException e) {
            System.out.println("Error leyendo el fichero: " + e.getMessage());
        }
    }

    public static void actualizaNumeroEmpleados(int dep, short nuevoEmpleados) {
        if (!esRegistroActivo(dep)) {
            System.out.println("Informativo: El departamento " + dep + " no existe y no se puede actualizar.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw")) {
            long offset = (dep - 1) * (long) RECORD_SIZE + 62;
            raf.seek(offset);
            raf.writeShort(nuevoEmpleados);
            System.out.println("Número de empleados actualizado correctamente.");
        } catch (IOException e) {
            System.out.println("Error de E/S: " + e.getMessage());
        }
    }

    public static void actualizaNombreLocalidad(int dep, String nombre, String localidad) {
        if (!esRegistroActivo(dep)) {
            System.out.println("Informativo: El departamento " + dep + " no existe y no se puede actualizar.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw")) {
            long offset = (dep - 1) * (long) RECORD_SIZE + 2;
            raf.seek(offset);
            raf.writeChars(nombre);
            raf.writeChars(localidad);
            System.out.println("Nombre y localidad actualizados correctamente.");
        } catch (IOException e) {
            System.out.println("Error de E/S: " + e.getMessage());
        }
    }

    public static void insertaDepartamento(int dep, String nombre, String localidad, short empleados) {
        if (esRegistroActivo(dep)) {
            System.out.println("Informativo: El departamento " + dep + " ya existe. No se ha modificado el fichero.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw")) {
            long posFinal = raf.length();
            long posDeseada = (dep - 1) * (long) RECORD_SIZE;

            if (posDeseada > posFinal) {
                raf.seek(posFinal);
                while (raf.getFilePointer() < posDeseada) {
                    raf.writeShort(0);
                    raf.writeChars("---------------");
                    raf.writeChars("---------------");
                    raf.writeShort(0);
                }
            }

            raf.seek(posDeseada);
            raf.writeShort(dep);
            raf.writeChars(nombre);
            raf.writeChars(localidad);
            raf.writeShort(empleados);

            System.out.println("Departamento insertado correctamente en posición " + dep + ".");
        } catch (IOException e) {
            System.out.println("Error de escritura: " + e.getMessage());
        }
    }

    public static void eliminaDepartamento(int dep) {
        if (!esRegistroActivo(dep)) {
            System.out.println("Informativo: El departamento " + dep + " no existe o ya estaba eliminado.");
            return;
        }

        System.out.print("¿Está seguro de que desea eliminar lógicamente el departamento " + dep + "? (S/N): ");
        String confirmacion = Entrada.cadena().trim();

        if (confirmacion.equalsIgnoreCase("S")) {
            try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw")) {
                long pos = (dep - 1) * (long) RECORD_SIZE;
                raf.seek(pos);
                raf.writeShort(-1);
                raf.writeChars("***************");
                raf.writeChars("***************");
                raf.writeShort(-1);
                System.out.println("Departamento " + dep + " eliminado.");
            } catch (IOException e) {
                System.out.println("Error durante la eliminación: " + e.getMessage());
            }
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    public static void listadoDepartamentos() {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            int cont = 0;
            System.out.println("--- LISTADO DE DEPARTAMENTOS ACTIVOS ---");

            // Recorrido secuencial byte a byte
            for (long pos = 0; pos < raf.length(); pos += RECORD_SIZE) {
                raf.seek(pos);
                short id = raf.readShort();
                String nom = leerString(raf, 15);
                String loc = leerString(raf, 15);
                short emp = raf.readShort();

                int idEsperado = (int) (pos / RECORD_SIZE) + 1;

                if (determinarEstado(idEsperado, id, nom, loc, emp) == EstadoPosicion.ACTIVA) {
                    System.out.printf("[ID: %4d] | Nom: %-15s | Loc: %-15s | Emp: %d\n",
                            id, nom.trim(), loc.trim(), emp);
                    cont++;
                }
            }
            if (cont == 0) {
                System.out.println("No existe ningún departamento activo en el fichero.");
            } else {
                System.out.println("Total departamentos mostrados: " + cont);
            }
        } catch (IOException e) {
            System.out.println("Error de E/S en listado: " + e.getMessage());
        }
    }

    public static void inspeccionFisica() {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long totalSlots = raf.length() / RECORD_SIZE;
            System.out.println("--- INSPECCIÓN FÍSICA DEL FICHERO ---");
            for (int i = 0; i < totalSlots; i++) {
                long posFisica = i * (long) RECORD_SIZE;
                int idEsperado = i + 1;

                raf.seek(posFisica);
                short id = raf.readShort();
                String nom = leerString(raf, 15);
                String loc = leerString(raf, 15);
                short emp = raf.readShort();

                EstadoPosicion estado = determinarEstado(idEsperado, id, nom, loc, emp);

                System.out.printf("Posición: %d | Desplazamiento: %d bytes | ID Esperado: %d | ID Leído: %d | Estado: %s\n",
                        i + 1, posFisica, idEsperado, id, estado.name());
            }
            if (totalSlots == 0) System.out.println("Fichero físicamente vacío.");
        } catch (IOException e) {
            System.out.println("Error de lectura: " + e.getMessage());
        }
    }

    public static void estadisticas() {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long length = raf.length();
            int slots = (int) (length / RECORD_SIZE);
            int activas = 0, eliminadas = 0, nuncaUsadas = 0, anomalias = 0;
            long totalEmpleados = 0;

            for (int i = 0; i < slots; i++) {
                int idEsperado = i + 1;
                raf.seek(i * (long) RECORD_SIZE);
                short id = raf.readShort();
                String nom = leerString(raf, 15);
                String loc = leerString(raf, 15);
                short emp = raf.readShort();

                switch (determinarEstado(idEsperado, id, nom, loc, emp)) {
                    case ACTIVA:
                        activas++;
                        totalEmpleados += emp;
                        break;
                    case ELIMINADA: eliminadas++; break;
                    case NUNCA_UTILIZADA: nuncaUsadas++; break;
                    case ANOMALA: anomalias++; break;
                }
            }

            System.out.println("--- ESTADÍSTICAS ---");
            System.out.println("Longitud del fichero (bytes):   " + length);
            System.out.println("Departamentos Activos:          " + activas);
            System.out.println("Posiciones Eliminadas:          " + eliminadas);
            System.out.println("Posiciones Nunca Utilizadas:    " + nuncaUsadas);
            System.out.println("Suma total de empleados:        " + totalEmpleados);

            int posicionesTotalesValidas = activas + eliminadas + nuncaUsadas;
            double porcentaje = posicionesTotalesValidas > 0 ? ((double) activas / posicionesTotalesValidas) * 100 : 0;
            System.out.printf("Porcentaje de ocupación:        %.2f%%\n", porcentaje);
            if (anomalias > 0) {
                System.out.println("ATENCIÓN: Existen " + anomalias + " posiciones con estado anómalo.");
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static boolean integridad(boolean silencioso) {
        boolean integro = true;
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long length = raf.length();

            if (length % RECORD_SIZE != 0) {
                if (!silencioso) System.out.println("Anomalía: La longitud (" + length + ") no es múltiplo de " + RECORD_SIZE + ".");
                integro = false;
            }

            long slots = length / RECORD_SIZE;
            for (int i = 0; i < slots; i++) {
                long pos = i * (long) RECORD_SIZE;
                int expected = i + 1;

                raf.seek(pos);
                short id = raf.readShort();
                String nom = leerString(raf, 15);
                String loc = leerString(raf, 15);
                short emp = raf.readShort();

                if (id < -1) {
                    if (!silencioso) System.out.println("Anomalía en byte " + pos + ": ID negativo no reconocido (" + id + ").");
                    integro = false;
                } else if (id > 0 && id != expected) {
                    if (!silencioso) System.out.println("Anomalía en byte " + pos + ": ID positivo (" + id + ") no coincide con su posición (" + expected + ").");
                    integro = false;
                } else if (id == expected) {
                    if (emp < 0) {
                        if (!silencioso) System.out.println("Anomalía en byte " + pos + ": Registro activo tiene empleados negativos.");
                        integro = false;
                    }
                    if (nom.trim().isEmpty() || loc.trim().isEmpty()) {
                        if (!silencioso) System.out.println("Anomalía en byte " + pos + ": Nombre o localidad vacíos en un registro activo.");
                        integro = false;
                    }
                } else if (id == 0) {
                    if (!nom.equals("---------------") || !loc.equals("---------------") || emp != 0) {
                        if (!silencioso) System.out.println("Anomalía en byte " + pos + ": Formato de marca 'Nunca Utilizada' incorrecto.");
                        integro = false;
                    }
                } else if (id == -1) {
                    if (!nom.equals("***************") || !loc.equals("***************") || emp != -1) {
                        if (!silencioso) System.out.println("Anomalía en byte " + pos + ": Formato de marca de 'Eliminación' incorrecto.");
                        integro = false;
                    }
                }
            }

            if (integro && !silencioso) {
                System.out.println("El fichero superó todas las pruebas de integridad sin problemas.");
            }
        } catch (IOException e) {
            System.out.println("Error de lectura durante el diagnóstico: " + e.getMessage());
            integro = false;
        }
        return integro;
    }


    private static boolean esRegistroActivo(int dep) {
        try (RandomAccessFile raf = new RandomAccessFile(FILENAME, "r")) {
            long pos = (dep - 1) * (long) RECORD_SIZE;
            if (pos >= raf.length()) return false;

            raf.seek(pos);
            short id = raf.readShort();
            String nom = leerString(raf, 15);
            String loc = leerString(raf, 15);
            short emp = raf.readShort();

            return determinarEstado(dep, id, nom, loc, emp) == EstadoPosicion.ACTIVA;
        } catch (IOException e) {
            return false;
        }
    }

    private static EstadoPosicion determinarEstado(int esperado, short id, String nom, String loc, short emp) {
        if (id == 0 && nom.equals("---------------") && loc.equals("---------------") && emp == 0) {
            return EstadoPosicion.NUNCA_UTILIZADA;
        }
        if (id == -1 && nom.equals("***************") && loc.equals("***************") && emp == -1) {
            return EstadoPosicion.ELIMINADA;
        }
        if (id == esperado && emp >= 0) {
            if (nom.trim().isEmpty() || loc.trim().isEmpty()) {
                return EstadoPosicion.ANOMALA;
            }
            return EstadoPosicion.ACTIVA;
        }
        return EstadoPosicion.ANOMALA;
    }

    private static String leerString(RandomAccessFile raf, int longitud) throws IOException {
        char[] array = new char[longitud];
        for (int i = 0; i < longitud; i++) {
            array[i] = raf.readChar();
        }
        return new String(array);
    }


    private static String solicitarTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String txt = Entrada.cadena().trim();
            if (txt.isEmpty()) {
                System.out.println(" -> Error: El texto no puede estar vacío ni formado solo por espacios.");
            } else if (txt.length() > 15) {
                System.out.println(" -> Error: El texto supera los 15 caracteres. No se admite truncamiento.");
            } else {
                return String.format("%-15s", txt);
            }
        }
    }


    private static int leerEntero(String mensaje, int min, int max) {
        while (true) {
            System.out.print(mensaje);
            try {
                int input = Entrada.entero();
                if (input >= min && input <= max) return input;
                System.out.println(" -> Error: Debe ser un número entre " + min + " y " + max + ".");
            } catch (Exception e) {
                System.out.println(" -> Error: Entrada no numérica válida.");
            }
        }
    }
}
