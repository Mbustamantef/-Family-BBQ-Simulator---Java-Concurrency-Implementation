
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

enum EstadoCarne {
    CRUDA("cruda"),
    SELLANDO("sellando"),
    PRIMERA_VUELTA("primera_vuelta"),
    SEGUNDA_VUELTA("segunda_vuelta"),
    LISTA("lista"),
    QUEMADA("quemada");

    private final String descripcion;
    EstadoCarne(String descripcion) { this.descripcion = descripcion; }
    @Override public String toString() { return descripcion; }
}

enum TipoCarne {
    CHORIZO("🌭"), MORCILLA("🖤"), COSTILLA("🥩"),
    VACIO("🥓"), POLLO("🐔");

    private final String emoji;
    TipoCarne(String emoji) { this.emoji = emoji; }
    @Override public String toString() { return emoji; }
}

// Clase que representa una pieza de carne
class PiezaCarne {
    private final TipoCarne tipo;
    private final String nombre;
    private volatile EstadoCarne estado;
    private volatile int tiempoCoccion;
    private volatile boolean robada;
    private volatile boolean condimentada;

    public PiezaCarne(TipoCarne tipo, String nombre) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.estado = EstadoCarne.CRUDA;
        this.tiempoCoccion = 0;
        this.robada = false;
        this.condimentada = false;
    }

    // Getters y setters thread-safe
    public synchronized EstadoCarne getEstado() { return estado; }
    public synchronized void setEstado(EstadoCarne estado) { this.estado = estado; }
    public synchronized boolean isRobada() { return robada; }
    public synchronized void setRobada(boolean robada) { this.robada = robada; }
    public synchronized boolean isCondimentada() { return condimentada; }
    public synchronized void setCondimentada(boolean condimentada) { this.condimentada = condimentada; }
    public synchronized void incrementarTiempo() { this.tiempoCoccion++; }

    public TipoCarne getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public int getTiempoCoccion() { return tiempoCoccion; }
}

// Clase principal que maneja la parrilla y recursos compartidos
class Parrilla {
    // Locks y semáforos para sincronización
    private final ReentrantLock mutexParrilla = new ReentrantLock();
    private final Semaphore semCerveza = new Semaphore(15);
    private final Semaphore semPinzaBuena = new Semaphore(1);
    private final Semaphore semCondimentos = new Semaphore(3);

    // Condiciones para notificaciones
    private final Condition condEmergencia = mutexParrilla.newCondition();
    private final Condition condCarneLista = mutexParrilla.newCondition();

    // Estado de la parrilla
    private final Map<String, PiezaCarne> carnes = new ConcurrentHashMap<>();
    private volatile int nivelCarbon = 100;
    private volatile int temperatura = 80;
    private volatile boolean asadoActivo = true;

    // Contadores de estadísticas
    private volatile int robosExitosos = 0;
    private volatile int intervencionesTios = 0;
    private volatile int condimentadasAbuela = 0;
    private volatile int conflictos = 0;

    // Lock para logging thread-safe
    private final Object lockLog = new Object();

    public Parrilla() {
        inicializarCarnes();
        iniciarControlTemperatura();
    }

    private void inicializarCarnes() {
        List<Map.Entry<TipoCarne, String>> carnesIniciales = Arrays.asList(
                new AbstractMap.SimpleEntry<>(TipoCarne.CHORIZO, "Chorizo1"),
                new AbstractMap.SimpleEntry<>(TipoCarne.CHORIZO, "Chorizo2"),
                new AbstractMap.SimpleEntry<>(TipoCarne.MORCILLA, "Morcilla1"),
                new AbstractMap.SimpleEntry<>(TipoCarne.MORCILLA, "Morcilla2"),
                new AbstractMap.SimpleEntry<>(TipoCarne.COSTILLA, "Costilla1"),
                new AbstractMap.SimpleEntry<>(TipoCarne.VACIO, "Vacio1"),
                new AbstractMap.SimpleEntry<>(TipoCarne.POLLO, "Pollo1")
        );

        for (Map.Entry<TipoCarne, String> entry : carnesIniciales) {
            carnes.put(entry.getValue(), new PiezaCarne(entry.getKey(), entry.getValue()));
        }
    }

    private void iniciarControlTemperatura() {
        // Hilo que controla la temperatura y el carbón
        Thread controlTemperatura = new Thread(() -> {
            while (asadoActivo) {
                try {
                    Thread.sleep(2000);
                    if (nivelCarbon > 0) {
                        nivelCarbon -= new Random().nextInt(3) + 1;
                        if (nivelCarbon < 30) {
                            logEvento("SISTEMA", "⚠️ Nivel de carbón bajo: " + nivelCarbon + "%", "YELLOW");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        controlTemperatura.setDaemon(true);
        controlTemperatura.start();
    }

    public void logEvento(String actor, String evento, String color) {
        synchronized (lockLog) {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String colorCode = getColorCode(color);
            String resetCode = color.isEmpty() ? "" : "\u001B[0m";
            System.out.println(colorCode + "[" + timestamp + "] " + actor + ": " + evento + resetCode);
        }
    }

    private String getColorCode(String color) {
        switch (color.toUpperCase()) {
            case "RED": return "\u001B[31m";
            case "GREEN": return "\u001B[32m";
            case "YELLOW": return "\u001B[33m";
            case "BLUE": return "\u001B[34m";
            case "PURPLE": return "\u001B[35m";
            case "CYAN": return "\u001B[36m";
            default: return "";
        }
    }

    public boolean accederParrilla(String actor, long timeoutMs) {
        try {
            if (mutexParrilla.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
                logEvento(actor, "🔥 Accede a la parrilla", "YELLOW");
                return true;
            } else {
                logEvento(actor, "⏰ No pudo acceder a la parrilla (timeout)", "RED");
                conflictos++;
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void liberarParrilla(String actor) {
        mutexParrilla.unlock();
        logEvento(actor, "🔓 Libera la parrilla", "GREEN");
    }

    public boolean tomarCerveza(String actor) {
        if (semCerveza.tryAcquire()) {
            logEvento(actor, "🍺 Toma una cerveza", "BLUE");
            return true;
        }
        logEvento(actor, "😢 No hay más cerveza disponible", "RED");
        return false;
    }

    public boolean usarPinzaBuena(String actor) {
        if (semPinzaBuena.tryAcquire()) {
            logEvento(actor, "🥄 Usa la pinza buena", "CYAN");
            return true;
        }
        return false;
    }

    public void liberarPinzaBuena(String actor) {
        semPinzaBuena.release();
        logEvento(actor, "🥄 Devuelve la pinza buena", "CYAN");
    }

    public boolean usarCondimentos(String actor) {
        if (semCondimentos.tryAcquire()) {
            logEvento(actor, "🧂 Usa condimentos", "GREEN");
            return true;
        }
        return false;
    }

    public void liberarCondimentos(String actor) {
        semCondimentos.release();
    }

    // Getters para acceso a datos
    public Map<String, PiezaCarne> getCarnes() { return carnes; }
    public int getNivelCarbon() { return nivelCarbon; }
    public int getTemperatura() { return temperatura; }
    public boolean isAsadoActivo() { return asadoActivo; }
    public void terminarAsado() { this.asadoActivo = false; }

    public synchronized void incrementarRobos() { robosExitosos++; }
    public synchronized void incrementarIntervenciones() { intervencionesTios++; }
    public synchronized void incrementarCondimentadas() { condimentadasAbuela++; }

    public void mostrarEstadisticas() {
        logEvento("ESTADISTICAS", "=== RESUMEN DEL ASADO ===", "PURPLE");
        logEvento("ESTADISTICAS", "Robos exitosos: " + robosExitosos, "");
        logEvento("ESTADISTICAS", "Intervenciones de tíos: " + intervencionesTios, "");
        logEvento("ESTADISTICAS", "Condimentadas por abuela: " + condimentadasAbuela, "");
        logEvento("ESTADISTICAS", "Conflictos: " + conflictos, "");

        long carneLista = carnes.values().stream().mapToLong(c ->
                c.getEstado() == EstadoCarne.LISTA ? 1 : 0).sum();
        long carneQuemada = carnes.values().stream().mapToLong(c ->
                c.getEstado() == EstadoCarne.QUEMADA ? 1 : 0).sum();

        logEvento("ESTADISTICAS", "Carne lista: " + carneLista + "/" + carnes.size(), "GREEN");
        logEvento("ESTADISTICAS", "Carne quemada: " + carneQuemada + "/" + carnes.size(), "RED");
    }
}

// Clase para el Asador Principal
class AsadorPrincipal implements Runnable {
    private final Parrilla parrilla;
    private final String nombre;
    private final Random random = new Random();

    public AsadorPrincipal(Parrilla parrilla, String nombre) {
        this.parrilla = parrilla;
        this.nombre = nombre;
    }

    @Override
    public void run() {
        parrilla.logEvento(nombre, "🎖️ Inicia como asador principal", "GREEN");

        while (parrilla.isAsadoActivo()) {
            try {
                // Verificar y cocinar carne
                if (parrilla.accederParrilla(nombre, 3000)) {
                    try {
                        cocinarCarne();
                        Thread.sleep(1000 + random.nextInt(2000)); // Tiempo de trabajo
                    } finally {
                        parrilla.liberarParrilla(nombre);
                    }
                }

                // Descanso entre acciones
                Thread.sleep(2000 + random.nextInt(3000));

                // Ocasionalmente tomar cerveza
                if (random.nextDouble() < 0.3) {
                    parrilla.tomarCerveza(nombre);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        parrilla.logEvento(nombre, "✅ Termina su trabajo como asador", "GREEN");
    }

    private void cocinarCarne() {
        for (PiezaCarne carne : parrilla.getCarnes().values()) {
            if (carne.isRobada()) continue;

            switch (carne.getEstado()) {
                case CRUDA:
                    if (random.nextDouble() < 0.4) {
                        carne.setEstado(EstadoCarne.SELLANDO);
                        parrilla.logEvento(nombre, "🔥 Empieza a sellar " + carne.getNombre(), "");
                    }
                    break;
                case SELLANDO:
                    if (carne.getTiempoCoccion() > 2) {
                        carne.setEstado(EstadoCarne.PRIMERA_VUELTA);
                        parrilla.logEvento(nombre, "🔄 Da vuelta " + carne.getNombre(), "");
                    }
                    break;
                case PRIMERA_VUELTA:
                    if (carne.getTiempoCoccion() > 4) {
                        carne.setEstado(EstadoCarne.SEGUNDA_VUELTA);
                        parrilla.logEvento(nombre, "🔄 Segunda vuelta a " + carne.getNombre(), "");
                    }
                    break;
                case SEGUNDA_VUELTA:
                    if (carne.getTiempoCoccion() > 6) {
                        if (random.nextDouble() < 0.8) {
                            carne.setEstado(EstadoCarne.LISTA);
                            parrilla.logEvento(nombre, "✅ " + carne.getNombre() + " está listo!", "GREEN");
                        } else {
                            carne.setEstado(EstadoCarne.QUEMADA);
                            parrilla.logEvento(nombre, "🔥💀 " + carne.getNombre() + " se quemó!", "RED");
                        }
                    }
                    break;
            }
            carne.incrementarTiempo();
        }
    }
}

// Clase para los Tíos "Expertos"
class TioExperto implements Runnable {
    private final Parrilla parrilla;
    private final String nombre;
    private final Random random = new Random();

    public TioExperto(Parrilla parrilla, String nombre) {
        this.parrilla = parrilla;
        this.nombre = nombre;
    }

    @Override
    public void run() {
        parrilla.logEvento(nombre, "🧔 Llega el tío experto", "BLUE");

        while (parrilla.isAsadoActivo()) {
            try {
                // Observar y decidir si intervenir
                if (random.nextDouble() < 0.3) { // 30% chance de intervenir
                    if (parrilla.accederParrilla(nombre, 1500)) {
                        try {
                            intervenir();
                            parrilla.incrementarIntervenciones();
                        } finally {
                            parrilla.liberarParrilla(nombre);
                        }
                    }
                } else {
                    // Solo observar y comentar
                    parrilla.logEvento(nombre, "🗣️ 'Esa carne necesita más fuego...'", "BLUE");
                }

                // Tomar cerveza frecuentemente
                if (random.nextDouble() < 0.5) {
                    parrilla.tomarCerveza(nombre);
                }

                Thread.sleep(3000 + random.nextInt(4000));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        parrilla.logEvento(nombre, "👋 Se retira el tío experto", "BLUE");
    }

    private void intervenir() {
        String[] consejos = {
                "Le da vuelta innecesariamente a una carne",
                "Ajusta la posición de los carbones",
                "Revisa el punto de cocción tocando la carne",
                "Mueve una pieza a zona menos caliente",
                "Comenta sobre la técnica del asador"
        };

        String consejo = consejos[random.nextInt(consejos.length)];
        parrilla.logEvento(nombre, "👨‍🍳 " + consejo, "BLUE");

        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Clase para los Primos Ladrones
class PrimoLadron implements Runnable {
    private final Parrilla parrilla;
    private final String nombre;
    private final Random random = new Random();

    public PrimoLadron(Parrilla parrilla, String nombre) {
        this.parrilla = parrilla;
        this.nombre = nombre;
    }

    @Override
    public void run() {
        parrilla.logEvento(nombre, "😈 Primo ladrón está al acecho", "PURPLE");

        while (parrilla.isAsadoActivo()) {
            try {
                // Buscar oportunidad de robo
                if (random.nextDouble() < 0.4) { // 40% chance de intentar robo
                    if (parrilla.accederParrilla(nombre, 800)) { // Timeout corto para ser sigiloso
                        try {
                            if (intentarRobo()) {
                                parrilla.incrementarRobos();
                            }
                        } finally {
                            parrilla.liberarParrilla(nombre);
                        }
                    }
                } else {
                    // Actuar inocente
                    parrilla.logEvento(nombre, "😇 Actúa inocentemente...", "PURPLE");
                }

                Thread.sleep(4000 + random.nextInt(6000)); // Espera entre intentos

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        parrilla.logEvento(nombre, "🏃‍♂️ Primo ladrón se escapa", "PURPLE");
    }

    private boolean intentarRobo() {
        // Buscar chorizo o morcilla lista para robar
        for (PiezaCarne carne : parrilla.getCarnes().values()) {
            if (!carne.isRobada() &&
                    (carne.getTipo() == TipoCarne.CHORIZO || carne.getTipo() == TipoCarne.MORCILLA) &&
                    (carne.getEstado() == EstadoCarne.SEGUNDA_VUELTA || carne.getEstado() == EstadoCarne.LISTA)) {

                carne.setRobada(true);
                parrilla.logEvento(nombre, "🥷 ROBA " + carne.getNombre() + " exitosamente!", "PURPLE");

                try {
                    Thread.sleep(200); // Tiempo de robo rápido
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }

        parrilla.logEvento(nombre, "😔 No encuentra nada bueno para robar", "PURPLE");
        return false;
    }
}

// Clase para la Abuela Supervisora
class AbuelaSupervisora implements Runnable {
    private final Parrilla parrilla;
    private final String nombre;
    private final Random random = new Random();

    public AbuelaSupervisora(Parrilla parrilla, String nombre) {
        this.parrilla = parrilla;
        this.nombre = nombre;
    }

    @Override
    public void run() {
        parrilla.logEvento(nombre, "👵 Abuela inicia supervisión", "CYAN");

        while (parrilla.isAsadoActivo()) {
            try {
                // Supervisar y condimentar
                if (random.nextDouble() < 0.6) { // 60% chance de intervenir
                    if (parrilla.usarCondimentos(nombre)) {
                        if (parrilla.accederParrilla(nombre, 2000)) {
                            try {
                                condimentarCarne();
                                parrilla.incrementarCondimentadas();
                            } finally {
                                parrilla.liberarParrilla(nombre);
                            }
                        }
                        parrilla.liberarCondimentos(nombre);
                    }
                } else {
                    // Solo supervisar
                    String[] comentarios = {
                            "Le falta sal a esa carne",
                            "tekaka la ejapo",
                            "Está muy seco, necesita más grasa",
                            "Así cocinaba tu abuelo",
                            "No le pongas tanta sal",
                            "Esa carne está perfecta"
                    };
                    String comentario = comentarios[random.nextInt(comentarios.length)];
                    parrilla.logEvento(nombre, "💬 '" + comentario + "'", "CYAN");
                }

                Thread.sleep(5000 + random.nextInt(5000));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        parrilla.logEvento(nombre, "👋 Abuela termina supervisión", "CYAN");
    }

    private void condimentarCarne() {
        for (PiezaCarne carne : parrilla.getCarnes().values()) {
            if (!carne.isRobada() && !carne.isCondimentada() &&
                    carne.getEstado() != EstadoCarne.CRUDA && carne.getEstado() != EstadoCarne.QUEMADA) {

                carne.setCondimentada(true);
                parrilla.logEvento(nombre, "🧂 Condimenta " + carne.getNombre(), "CYAN");

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break; // Solo condimenta una pieza por vez
            }
        }
    }
}

// Clase principal del simulador
public class SimuladorAsadoFamiliar {
    public static void main(String[] args) {
        System.out.println("🔥🥩 === SIMULADOR DE PARRILLA EN ASADO FAMILIAR === 🥩🔥");
        System.out.println("Presiona Ctrl+C para terminar el asado en cualquier momento\n");

        // Crear la parrilla (recurso compartido)
        Parrilla parrilla = new Parrilla();

        // Crear lista de hilos
        List<Thread> actores = new ArrayList<>();

        // Crear asador principal
        actores.add(new Thread(new AsadorPrincipal(parrilla, "👨‍🍳 ASADOR PRINCIPAL")));

        // Crear tíos expertos
        actores.add(new Thread(new TioExperto(parrilla, "👨‍🦳 TIO ALFONSO")));
        actores.add(new Thread(new TioExperto(parrilla, "👨‍🦲 TIO ARIEL")));
        actores.add(new Thread(new TioExperto(parrilla, "🧔 TIO RODRIGO")));

        // Crear primos ladrones
        actores.add(new Thread(new PrimoLadron(parrilla, "😈 PRIMO MAXI")));
        actores.add(new Thread(new PrimoLadron(parrilla, "🥷 PRIMO SEBASTIAN")));

        // Crear abuela supervisora
        actores.add(new Thread(new AbuelaSupervisora(parrilla, "👵 ABUELA VIVI")));

        // Hook para terminar gracefully con Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            parrilla.logEvento("SISTEMA", "🛑 Terminando asado...", "RED");
            parrilla.terminarAsado();

            // Esperar que terminen los hilos
            for (Thread actor : actores) {
                try {
                    actor.interrupt();
                    actor.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            parrilla.mostrarEstadisticas();
            System.out.println("\n🎉 ¡Gracias por participar del asado familiar! 🎉");
        }));

        // Iniciar todos los hilos
        for (Thread actor : actores) {
            actor.start();
        }

        // Hilo principal controla duración del asado
        try {
            Thread.sleep(60000); // 60 segundos de asado
            parrilla.logEvento("SISTEMA", "⏰ Tiempo de asado terminado", "YELLOW");
            parrilla.terminarAsado();

            // Esperar que terminen todos los hilos
            for (Thread actor : actores) {
                actor.join();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        parrilla.mostrarEstadisticas();
        System.out.println("\n🎉 ¡Asado familiar completado! 🎉");
    }
}