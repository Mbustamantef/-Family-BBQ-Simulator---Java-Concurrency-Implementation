# 🔥🥩 Family BBQ Simulator - Java Concurrency Implementation

A multithreaded simulator that recreates the chaotic dynamics of an Argentine family barbecue (asado), implementing advanced concepts of **concurrency**, **synchronization**, and **parallel programming** in Java.

## 🎯 Overview

This project simulates a family barbecue where multiple actors interact simultaneously with shared resources (grill, beer, condiments), generating realistic concurrency situations that require synchronization to avoid race conditions and conflicts.

## 🏗️ Concurrency Concepts Implemented

### 🔒 Synchronization Mechanisms

| Mechanism | Use in Simulator | Protected Resource |
|-----------|-----------------|-------------------|
| **ReentrantLock** | Exclusive grill control | Access to cook meat |
| **Semaphore** | Resource availability limits | Beer (15), Good tongs (1), Condiments (3) |
| **Condition Variables** | Event notifications | Emergencies and meat status |
| **ConcurrentHashMap** | Thread-safe collections | Meat state management |
| **Volatile** | Memory visibility | Shared state variables |

### 👥 Actor Types (Threads)

#### 🎖️ Main Griller (`AsadorPrincipal`)
- **Role**: Primary cook responsible for meat preparation
- **Behavior**:
  - Controls cooking stages (raw → searing → first turn → second turn → ready)
  - Uses exclusive grill access with timeout
  - Occasionally drinks beer
- **Concurrency**: Uses ReentrantLock with timeout for grill access

#### 🧔 Expert Uncles (`TioExperto`)
- **Role**: "Know-it-all" relatives who interfere with cooking
- **Behavior**:
  - Randomly intervenes with cooking process
  - Gives unsolicited advice
  - Frequently drinks beer
- **Concurrency**: Competes for grill access with shorter timeouts

#### 😈 Thief Cousins (`PrimoLadron`)
- **Role**: Sneaky relatives who steal ready meat
- **Behavior**:
  - Attempts to steal chorizo or morcilla when ready
  - Uses short timeouts to be stealthy
  - Acts innocent between attempts
- **Concurrency**: Quick grill access attempts to avoid detection

#### 👵 Supervisor Grandmother (`AbuelaSupervisora`)
- **Role**: Family matriarch who seasons and supervises
- **Behavior**:
  - Adds condiments to cooking meat
  - Provides cooking commentary
  - Uses semaphore-controlled condiment access
- **Concurrency**: Semaphore for limited condiment access

## 🥩 Meat State Machine

```
CRUDA (Raw) → SELLANDO (Searing) → PRIMERA_VUELTA (First Turn)
    → SEGUNDA_VUELTA (Second Turn) → LISTA (Ready) / QUEMADA (Burnt)
```

### Meat Types
- 🌭 **CHORIZO** - High theft target
- 🖤 **MORCILLA** - High theft target
- 🥩 **COSTILLA** - Premium cut
- 🥓 **VACIO** - Popular choice
- 🐔 **POLLO** - Chicken option

## 🛠️ Technical Implementation

### Thread-Safe Classes

#### `Parrilla` (Main Controller)
```java
public class Parrilla {
    private final ReentrantLock mutexParrilla = new ReentrantLock();
    private final Semaphore semCerveza = new Semaphore(15);
    private final Semaphore semPinzaBuena = new Semaphore(1);
    private final Semaphore semCondimentos = new Semaphore(3);

    private final Map<String, PiezaCarne> carnes = new ConcurrentHashMap<>();
    private volatile int nivelCarbon = 100;
    private volatile boolean asadoActivo = true;
}
```

#### `PiezaCarne` (Thread-Safe Meat)
```java
public class PiezaCarne {
    private volatile EstadoCarne estado;
    private volatile boolean robada;
    private volatile boolean condimentada;

    public synchronized EstadoCarne getEstado() { return estado; }
    public synchronized void setEstado(EstadoCarne estado) { this.estado = estado; }
}
```

### Resource Management

#### Semaphore Usage
- **Beer Semaphore** (15 permits): Controls beer consumption
- **Good Tongs Semaphore** (1 permit): Exclusive access to quality utensil
- **Condiments Semaphore** (3 permits): Limited seasoning access

#### Lock Strategy
- **ReentrantLock with Timeout**: Prevents deadlocks in grill access
- **Synchronized Methods**: Protects meat state changes
- **Volatile Variables**: Ensures memory visibility for flags

## 🚀 Getting Started

### Prerequisites
- **Java 11+** (uses modern concurrency features)
- Any Java IDE or command line

### Compilation and Execution

```bash
# Compile
javac SimuladorAsadoFamiliar.java

# Run
java SimuladorAsadoFamiliar
```

### Runtime Controls
- **Ctrl+C**: Gracefully terminates the BBQ and shows statistics
- **Default Duration**: 60 seconds of simulation
- **Real-time Logging**: Color-coded events with timestamps

## 📊 Output Example

### Real-time Events
```
[14:23:15] 👨‍🍳 ASADOR PRINCIPAL: 🔥 Accede a la parrilla
[14:23:16] 👨‍🍳 ASADOR PRINCIPAL: 🔥 Empieza a sellar Chorizo1
[14:23:17] 🧔 TIO RODRIGO: 🍺 Toma una cerveza
[14:23:18] 😈 PRIMO MAXI: 🥷 ROBA Morcilla2 exitosamente!
[14:23:19] 👵 ABUELA VIVI: 🧂 Condimenta Costilla1
```

### Final Statistics
```
=== RESUMEN DEL ASADO ===
Robos exitosos: 3
Intervenciones de tíos: 7
Condimentadas por abuela: 5
Conflictos: 2
Carne lista: 4/7
Carne quemada: 1/7
```

## 🎓 Educational Value

### Learning Objectives
- **Multithreading**: Multiple actors running concurrently
- **Synchronization**: Preventing race conditions
- **Resource Management**: Controlling access to limited resources
- **Deadlock Prevention**: Using timeouts and proper lock ordering
- **Producer-Consumer**: Meat cooking and consumption patterns
- **Thread Communication**: Condition variables and notifications

### Concurrency Challenges Demonstrated
1. **Race Conditions**: Multiple threads modifying meat state
2. **Resource Contention**: Competition for grill access
3. **Starvation**: Fair access to shared resources
4. **Deadlock Prevention**: Timeout-based lock acquisition
5. **Memory Consistency**: Volatile variables for visibility

## 🔧 Customization Options

### Adjusting Parameters
```java
// Modify resource limits
private final Semaphore semCerveza = new Semaphore(20); // More beer

// Change simulation duration
Thread.sleep(120000); // 2 minutes instead of 1

// Add more actors
actores.add(new Thread(new TioExperto(parrilla, "TIO CARLOS")));
```

### Adding New Actor Types
```java
class NuevoActor implements Runnable {
    // Implement your own family member behavior
    // Use existing synchronization mechanisms
}
```

## 🚧 Advanced Features

### Error Handling
- **InterruptedException**: Graceful thread termination
- **Timeout Management**: Prevents indefinite blocking
- **Resource Cleanup**: Proper lock and semaphore release

### Performance Monitoring
- **Conflict Tracking**: Counts synchronization failures
- **Success Metrics**: Measures actor effectiveness
- **Resource Utilization**: Monitors semaphore usage

### Thread Safety Guarantees
- **Data Race Prevention**: All shared data properly synchronized
- **Atomicity**: Critical sections protected by locks
- **Visibility**: Volatile variables ensure memory consistency
- **Ordering**: Happens-before relationships established

## 🎭 Cultural Context

This simulator captures the authentic chaos of Paraguay family barbecues:
- **Asador Principal**: The designated cook (usually male)
- **Tíos**: Uncles who "know better" and interfere
- **Primos**: Young cousins who sneak food
- **Abuela**: Grandmother who seasons everything
- **Hierarchical Dynamics**: Realistic family interactions

## 📚 Technical References

### Java Concurrency APIs Used
- `java.util.concurrent.locks.ReentrantLock`
- `java.util.concurrent.Semaphore`
- `java.util.concurrent.ConcurrentHashMap`
- `java.util.concurrent.TimeUnit`
- `volatile` keyword for memory visibility
- `synchronized` methods for atomic operations

### Design Patterns
- **Producer-Consumer**: Meat cooking and consumption
- **Reader-Writer**: Grill state monitoring and modification
- **Resource Pool**: Limited beer and condiment access
- **Observer**: Event logging and monitoring

## 🤝 Contributing

Enhance the simulator by:
1. Adding new family member types
2. Implementing additional synchronization mechanisms
3. Creating more complex cooking scenarios
4. Adding performance metrics
5. Implementing distributed simulation

## 📄 License

This project is created for educational purposes to demonstrate Java concurrency concepts. Feel free to use, modify, and distribute for learning concurrent programming.

---

**Built with ☕ Java Concurrency APIs - Perfect for Operating Systems and Concurrent Programming Courses! 🎓**
