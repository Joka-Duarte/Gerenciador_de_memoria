
import java.util.Random;
import java.util.concurrent.BlockingQueue;

// Classe Gerador de Requisições
public class GeradorRequisicoes implements Runnable {
    private final BlockingQueue<Requisicao> fila;
    private final int total;
    private final int min;
    private final int max;

    public GeradorRequisicoes(BlockingQueue<Requisicao> fila, int total, int min, int max) {
        this.fila = fila;
        this.total = total;
        this.min = min;
        this.max = max;
    }

    @Override
    public void run() {
        Random rand = new Random();
        for (int i = 1; i <= total; i++) {
            int tamanho = rand.nextInt(max - min + 1) + min;
            try {
                fila.put(new Requisicao(i, tamanho));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
