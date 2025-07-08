
import java.util.ArrayList;
import java.util.List;      
import java.util.Random;
import java.util.concurrent.BlockingQueue;

// Classe Gerador de Requisições
public class GeradorRequisicoes implements Runnable {
    private final BlockingQueue<Requisicao> fila;
    private final int total;
    private final int min;
    private final int max;

    // Construtor
    public GeradorRequisicoes(BlockingQueue<Requisicao> fila, int total, int min, int max) {
        this.fila = fila;
        this.total = total;
        this.min = min;
        this.max = max;
    }

    /**
     * Método executado pela thread na versão paralela.
     * Gera requisições e as insere na BlockingQueue.
     */
    @Override
    public void run() {
        Random rand = new Random();
        for (int i = 1; i <= total; i++) {
            int tamanho = rand.nextInt(max - min + 1) + min;
            try {
                // A fila pode ser nula na execução sequencial, então verificamos.
                if (fila != null) {
                    fila.put(new Requisicao(i, tamanho));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

     //Uma lista contendo todas as requisições geradas.
    //Método Paralelo
    public List<Requisicao> gerarTodasDeUmaVez() {
        // Assegura que List e ArrayList são reconhecidos pelos imports.
        List<Requisicao> todasAsRequisicoes = new ArrayList<>(total);
        Random rand = new Random();
        for (int i = 1; i <= total; i++) {
            int tamanho = rand.nextInt(max - min + 1) + min;
            todasAsRequisicoes.add(new Requisicao(i, tamanho));
        }
        return todasAsRequisicoes;
    }
}