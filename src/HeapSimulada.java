
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/*
  Representa a memória principal (Heap) da simulação.
  Gerencia a alocação, liberação e compactação da memória de forma concorrente e segura.
 */
public class HeapSimulada {
    private final int[] heap;
    private final Queue<Requisicao> fifo = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final EstrategiaAlocacao estrategia;
    private final boolean modoVerificacao;

    private int totalAlocadas = 0;
    private long totalBytesAlocados = 0;
    private int totalRemovidas = 0;
    private int totalCompactacoes = 0;
    /*
      Construtor da HeapSimulada.
      @param tamanhoKB O tamanho da heap em kilobytes.
      @param estrategia O algoritmo de alocação a ser usado (First-Fit, Best-Fit, Worst-Fit).
      @param modoVerificacao Se true, ativa os testes de integridade após operações críticas.
     */
    public HeapSimulada(int tamanhoKB, EstrategiaAlocacao estrategia, boolean modoVerificacao) {
        this.heap = new int[tamanhoKB * 1024 / 4];
        this.estrategia = estrategia;
        this.modoVerificacao = modoVerificacao;
        System.out.println("\nHeap iniciada com a estrategia: " + estrategia);
        if (this.modoVerificacao) {
            System.out.println(">>> MODO DE VERIFICACAO ATIVADO <<<");
        }
    }
    
    /*
      Tenta alocar uma requisição na heap. Se não houver espaço, dispara a liberação de memória e a compactação.
     */
    public boolean alocar(Requisicao r) {
        lock.lock();
        try {
        int inicio = buscarEspacoContiguo(r.blocosNecessarios);
        // Se não encontrou espaço na primeira tentativa, inicia o processo de limpeza.
        if (inicio == -1) {
            // 1. PRIMEIRO, executamos a liberação de memória para criar espaço.
            liberarMemoria();
            long blocosAposLiberacao = 0;
            // Se o modo de verificação estiver ativo, preparamos o teste.
            if (this.modoVerificacao) {
                // 2. AGORA, contamos os blocos, DEPOIS da liberação e ANTES da compactação.
                // Isso isola o teste para validar apenas a operação de compactar.
                blocosAposLiberacao = Arrays.stream(heap).filter(val -> val != 0).count();
            }
            // 3. Em seguida, executamos a compactação para juntar os espaços livres.
            compactar();
            // 4. E finalmente, se o modo estiver ativo, verificamos se a compactação foi bem-sucedida.
            if (this.modoVerificacao) {
                if (!verificarAposCompactacao(blocosAposLiberacao)) {
                    // A verificação agora compara estados consistentes e não deve mais falhar.
                    System.err.println("!! ERRO CRITICO NA COMPACTACAO DETECTADO. O estado da heap pode estar corrompido.");
                }
            }
            // Tenta encontrar espaço novamente após todo o processo de limpeza.
            inicio = buscarEspacoContiguo(r.blocosNecessarios);
            if (inicio == -1) {
                return false; // Falha mesmo após a limpeza.
            }
        }
        // Se encontrou espaço (seja na primeira tentativa ou depois da limpeza), aloca aqui.
        for (int i = 0; i < r.blocosNecessarios; i++) {
            heap[inicio + i] = r.id;
        }
        fifo.add(r);
        totalAlocadas++;
        totalBytesAlocados += r.tamanhoBytes;
        return true;

        } finally {
            lock.unlock();
        }
    }

    // Seletor para os tipos de algoritmos de alocação
    private int buscarEspacoContiguo(int blocos) {
        switch (estrategia) {
            case FIRST_FIT -> {
                return buscarComFirstFit(blocos);
            }
            case BEST_FIT -> {
                return buscarComBestFit(blocos);
            }
            case WORST_FIT -> {
                return buscarComWorstFit(blocos);
            }
            default -> throw new IllegalStateException("Estrategia de alocacao desconhecida: " + estrategia);
        }
    }
    
    // Metodo First Fit
    private int buscarComFirstFit(int blocos) {
        int livres = 0;
        int inicio = -1;
        for (int i = 0; i < heap.length; i++) {
            if (heap[i] == 0) {
                if (livres == 0) inicio = i;
                livres++;
                if (livres == blocos) return inicio;
            } else {
                livres = 0;
            }
        }
        return -1;
    }

    // Metodo Worst Fit
    private int buscarComWorstFit(int blocos) {
        int piorInicio = -1;
        int piorTamanho = -1;
        int i = 0;
        while (i < heap.length) {
            if (heap[i] == 0) {
                int inicio = i;
                int tamanho = 0;
                while (i < heap.length && heap[i] == 0) {
                    tamanho++;
                    i++;
                }
                if (tamanho >= blocos && tamanho > piorTamanho) {
                    piorInicio = inicio;
                    piorTamanho = tamanho;
                }
            } else {
                i++;
            }
        }
        return piorInicio;
    }

    // Metodo Best Fit
    private int buscarComBestFit(int blocos) {
        int melhorInicio = -1;
        int melhorTamanho = Integer.MAX_VALUE;
        int i = 0;
        while (i < heap.length) {
            if (heap[i] == 0) {
                int inicio = i;
                int tamanho = 0;
                while (i < heap.length && heap[i] == 0) {
                    tamanho++;
                    i++;
                }
                if (tamanho >= blocos && tamanho < melhorTamanho) {
                    melhorInicio = inicio;
                    melhorTamanho = tamanho;
                }
            } else {
                i++;
            }
        }
        return melhorInicio;
    }

    /**
     * Libera memória removendo as requisições mais antigas (FIFO) até que
     * uma meta de liberação (30% da heap) seja atingida.
     */
    private void liberarMemoria() {
        int blocosParaLiberar = (int) (heap.length * 0.3);
        int liberados = 0;
        while (!fifo.isEmpty() && liberados < blocosParaLiberar) {
            Requisicao r = fifo.poll();
            for (int i = 0; i < heap.length; i++) {
                if (heap[i] == r.id) {
                    heap[i] = 0;
                    liberados++;
                }
            }
            totalRemovidas++;
        }
    }

    /**
     * Compacta a heap usando um algoritmo "in-place" com dois ponteiros,
     * que não requer a alocação de um array extra. É mais eficiente em memória.
     */
    private void compactar() {
        int ponteiroEscrita = 0;
        int ponteiroLeitura = 0;
        while (ponteiroLeitura < heap.length) {
            if (heap[ponteiroLeitura] != 0) {
                heap[ponteiroEscrita] = heap[ponteiroLeitura];
                ponteiroEscrita++;
            }
            ponteiroLeitura++;
        }
        while (ponteiroEscrita < heap.length) {
            heap[ponteiroEscrita] = 0;
            ponteiroEscrita++;
        }
        totalCompactacoes++;
    }

    // --- MÉTODOS DE VERIFICAÇÃO E DIAGNÓSTICO ---
    /*
      Realiza uma verificação automatizada da heap para garantir que a compactação
      foi bem-sucedida. Verifica a perda de dados e a estrutura da heap.
      @param blocosOcupadosAntes A contagem de blocos que estavam ocupados ANTES da compactação.
      @return true se a heap passou na verificação, false caso contrário.
     */
    private boolean verificarAposCompactacao(long blocosOcupadosAntes) {
        long blocosOcupadosDepois = Arrays.stream(heap).filter(val -> val != 0).count();
        if (blocosOcupadosAntes != blocosOcupadosDepois) {
            System.err.printf("!! FALHA DE INTEGRIDADE: Perda de dados na compactacao. Antes: %d, Depois: %d%n", blocosOcupadosAntes, blocosOcupadosDepois);
            return false;
        }
        boolean encontrouBlocoLivre = false;
        for (int i = 0; i < heap.length; i++) {
            if (heap[i] == 0) {
                encontrouBlocoLivre = true;
            }
            if (encontrouBlocoLivre && heap[i] != 0) {
                System.err.println("!! FALHA ESTRUTURAL: Bloco ocupado encontrado apos espaço livre. A heap nao esta compactada.");
                return false;
            }
        }
        return true;
    }
    
    /*
      Imprime o estado atual do array da heap no console.
      Usado como ferramenta de diagnóstico quando uma verificação de integridade falha.
     */
    public void imprimirEstadoHeap(String titulo) {
        System.out.println("\n----- " + titulo + " -----");
        for (int i = 0; i < heap.length; i++) {
            System.out.print(heap[i] + " ");
            if ((i + 1) % 50 == 0 || i == heap.length - 1) {
                System.out.println();
            }
        }
        System.out.println("----------------------------------------");
    }
    /**
      Imprime os resultados finais e as métricas da simulação.
      @param tempoTotalMs
     */
    public void imprimirResultado(long tempoTotalMs) {
        System.out.println("\n----- RESULTADOS FINAIS -----");
        System.out.println("Estrategia de Alocacao Utilizada: " + estrategia);
        System.out.println("Total de requisicao atendidas: " + totalAlocadas);
        System.out.println("Tamanho medio das variaveis alocadas: " + (totalAlocadas > 0 ? (totalBytesAlocados / totalAlocadas) : 0) + " bytes");
        System.out.println("Total de variaveis removidas (FIFO): " + totalRemovidas);
        System.out.println("Chamadas ao algoritmo de compactacao: " + totalCompactacoes);
        System.out.println("Tempo total de execucao: " + tempoTotalMs + " ms");
    }
    
}