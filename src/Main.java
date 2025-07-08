
// Trabalho Final - Simulador de Gerenciador de Memoria Dinamica
// Integrantes Breno Henrique & João Duarte


import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // --- SEÇÃO DE CONFIGURAÇÃO
        Scanner sc = new Scanner(System.in);

        System.out.println("----- CONFIGURACAO DA SIMULACAO -----");
        System.out.print("Informe o tamanho da heap (em KB): ");
        int heapKB = sc.nextInt();
        System.out.print("Informe o tamanho minimo da variavel (bytes): ");
        int min = sc.nextInt();
        System.out.print("Informe o tamanho maximo da variavel (bytes): ");
        int max = sc.nextInt();
        System.out.print("Informe o numero total de requisicoes: ");
        int total = sc.nextInt();

        System.out.println("\nEscolha a estrategia de alocacao:");
        System.out.println("1 - First Fit\n2 - Worst Fit\n3 - Best Fit");
        System.out.print("Opcao: ");
        int escolhaEstrategia = sc.nextInt();
        EstrategiaAlocacao estrategia;
        switch (escolhaEstrategia) {
            case 1 -> estrategia = EstrategiaAlocacao.FIRST_FIT;
            case 2 -> estrategia = EstrategiaAlocacao.WORST_FIT;
            case 3 -> estrategia = EstrategiaAlocacao.BEST_FIT;
            default -> {
                System.out.println("Opcao invalida. Usando First-Fit como padrao.");
                estrategia = EstrategiaAlocacao.FIRST_FIT;
            }
        }

        // --- SEÇÃO DE SELEÇÃO DO MODO DE EXECUÇÃO ---
        System.out.println("\nEscolha o modo de execucao:");
        System.out.println("1 - Sequencial (Single-Thread)");
        System.out.println("2 - Paralelo (Multi-Thread)");
        System.out.print("Opcao: ");
        int escolhaModo = sc.nextInt();
        
        // Cria a Heap. Ela é usada em ambos os modos.
        // O modo de verificação pode ser ativado para qualquer um dos dois.
        System.out.print("\nAtivar modo de verificacao da heap? (S/N): ");
        boolean modoVerificacao = sc.next().equalsIgnoreCase("S");
        HeapSimulada heap = new HeapSimulada(heapKB, estrategia, modoVerificacao);

        // Chama a rotina apropriada com base na escolha do usuário
        if (escolhaModo == 1) {
            executarVersaoSequencial(total, min, max, heap);
        } else {
            System.out.print("Informe o numero de threads para o modo paralelo: ");
            int threads = sc.nextInt();
            executarVersaoParalela(total, min, max, threads, heap);
        }
        
        // Pergunta final para inspecionar a heap
        System.out.print("\nDeseja imprimir o estado final da heap? (S/N): ");
        if (sc.next().equalsIgnoreCase("S")) {
            heap.imprimirEstadoHeap("ESTADO FINAL DA HEAP");
        }
        
        sc.close();
        System.out.println("\nSimulacao concluida.");
    }

    /**
     * Executa a simulação de forma totalmente sequencial, em uma única thread.
     */
    private static void executarVersaoSequencial(int total, int min, int max, HeapSimulada heap) {
        System.out.println("\n--- INICIANDO EXECUCAO SEQUENCIAL ---");
        
        // 1. Gera todas as requisições de uma vez, em memória.
        GeradorRequisicoes gerador = new GeradorRequisicoes(null, total, min, max); // A fila não é necessária aqui
        List<Requisicao> todasAsRequisicoes = gerador.gerarTodasDeUmaVez();

        long inicio = System.currentTimeMillis();
        
        // 2. Processa cada requisição, uma por uma, na thread principal.
        for (Requisicao r : todasAsRequisicoes) {
            heap.alocar(r);
        }
        
        long fim = System.currentTimeMillis();
        heap.imprimirResultado(fim - inicio);
    }

    /**
     * Executa a simulação de forma paralela, usando o padrão Produtor-Consumidor.
     */
    private static void executarVersaoParalela(int total, int min, int max, int threads, HeapSimulada heap) throws InterruptedException {
        System.out.println("\n--- INICIANDO EXECUCAO PARALELA COM " + threads + " THREADS ---");

        BlockingQueue<Requisicao> fila = new LinkedBlockingQueue<>();
        Thread gerador = new Thread(new GeradorRequisicoes(fila, total, min, max));
        
        long inicio = System.currentTimeMillis();
        gerador.start();

        Thread[] alocadores = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            alocadores[i] = new Thread(new Alocador(fila, heap));
            alocadores[i].start();
        }

        gerador.join();
        for (int i = 0; i < threads; i++) {
            fila.put(new Requisicao(-1, 0));
        }
        for (Thread t : alocadores) t.join();

        long fim = System.currentTimeMillis();
        heap.imprimirResultado(fim - inicio);
    }
}