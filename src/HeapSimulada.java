
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

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

    public HeapSimulada(int tamanhoKB, EstrategiaAlocacao estrategia, boolean modoVerificacao) {
        this.heap = new int[tamanhoKB * 1024 / 4];
        this.estrategia = estrategia;
        this.modoVerificacao = modoVerificacao;
        System.out.println("\nHeap iniciada com a estrategia: " + estrategia);
        if (this.modoVerificacao) {
            System.out.println(">>> MODO DE VERIFICAÇÃO ATIVADO <<<");
        }
    }

    public boolean alocar(Requisicao r) {
        lock.lock();
        try {
            int inicio = buscarEspacoContiguo(r.blocosNecessarios);
            if (inicio == -1) {
                long blocosAntes = 0;
                if (this.modoVerificacao) {
                    blocosAntes = Arrays.stream(heap).filter(val -> val != 0).count();
                }
                liberarMemoria();
                compactar();
                if (this.modoVerificacao) {
                    if (!verificarAposCompactacao(blocosAntes)) {
                        System.err.println("!! ERRO CRÍTICO NA COMPACTAÇÃO. O estado da heap pode estar corrompido.");
                        /*System.err.println("!! EXIBINDO ESTADO DA HEAP APÓS A FALHA PARA DIAGNÓSTICO:");
                        imprimirEstadoHeap("ESTADO DA HEAP CORROMPIDO");*/
                    }
                }
                inicio = buscarEspacoContiguo(r.blocosNecessarios);
                if (inicio == -1) {
                    return false;
                }
            }
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
            default -> throw new IllegalStateException("Estratégia de alocação desconhecida: " + estrategia);
        }
    }
    
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

    private boolean verificarAposCompactacao(long blocosOcupadosAntes) {
        long blocosOcupadosDepois = Arrays.stream(heap).filter(val -> val != 0).count();
        if (blocosOcupadosAntes != blocosOcupadosDepois) {
            System.err.printf("!! FALHA DE INTEGRIDADE: Perda de dados na compactação. Antes: %d, Depois: %d%n", blocosOcupadosAntes, blocosOcupadosDepois);
            return false;
        }
        boolean encontrouBlocoLivre = false;
        for (int i = 0; i < heap.length; i++) {
            if (heap[i] == 0) {
                encontrouBlocoLivre = true;
            }
            if (encontrouBlocoLivre && heap[i] != 0) {
                System.err.println("!! FALHA ESTRUTURAL: Bloco ocupado encontrado após espaço livre. A heap não está compactada.");
                return false;
            }
        }
        return true;
    }

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

    public void imprimirResultado(long tempoTotalMs) {
        System.out.println("\n----- RESULTADOS FINAIS -----");
        System.out.println("Estratégia de Alocação Utilizada: " + estrategia);
        System.out.println("Total de requisições atendidas: " + totalAlocadas);
        System.out.println("Tamanho médio das variáveis alocadas: " + (totalAlocadas > 0 ? (totalBytesAlocados / totalAlocadas) : 0) + " bytes");
        System.out.println("Total de variáveis removidas (FIFO): " + totalRemovidas);
        System.out.println("Chamadas ao algoritmo de compactação: " + totalCompactacoes);
        System.out.println("Tempo total de execução: " + tempoTotalMs + " ms");
    }
    
}