
import java.util.concurrent.BlockingQueue;

// Classe Alocador
public class Alocador implements Runnable {
    private final BlockingQueue<Requisicao> fila;
    private final HeapSimulada heap;

    public Alocador(BlockingQueue<Requisicao> fila, HeapSimulada heap) {
        this.fila = fila;
        this.heap = heap;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Requisicao r = fila.take();
                if (r.id == -1) break;
                heap.alocar(r);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
