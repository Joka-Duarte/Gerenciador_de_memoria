
// Trabalho Final - Simulador de Gerenciador de Memoria Dinamica
// Integrantes Breno Henrique & João Duarte


import java.util.Scanner;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int heapKB;
        int min;
        int max;
        int total;
        int threads;
        EstrategiaAlocacao estrategia;
        boolean modoVerificacao;

        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Informe o tamanho da heap (em KB): ");
            heapKB = sc.nextInt();
            System.out.print("Informe o tamanho minimo da variavel (bytes): ");
            min = sc.nextInt();
            System.out.print("Informe o tamanho maximo da variavel (bytes): ");
            max = sc.nextInt();
            System.out.print("Informe o numero total de requisicoes: ");
            total = sc.nextInt();
            System.out.print("Informe o numero de threads: ");
            threads = sc.nextInt();

            // ----- NOVA SEÇÃO: SELEÇÃO DA ESTRATÉGIA -----
            System.out.println("\nEscolha a estrategia de alocacao:");
            System.out.println("1 - First Fit");
            System.out.println("2 - Worst Fit");
            System.out.println("3 - Best Fit");
            System.out.print("Opcao: ");
            int escolha = sc.nextInt();

            switch (escolha) {
                case 1 -> estrategia = EstrategiaAlocacao.FIRST_FIT;
                case 2 -> estrategia = EstrategiaAlocacao.WORST_FIT;
                case 3 -> estrategia = EstrategiaAlocacao.BEST_FIT;
                default -> {
                    System.out.println("Opcao invalida. Usando First-Fit como padrao.");
                    estrategia = EstrategiaAlocacao.FIRST_FIT;
                }
            }
            // Pergunta ao usuário e atribui um valor à variável 'modoVerificacao'
            System.out.print("\nAtivar modo de verificação da heap? (S/N): ");
            String respostaVerificacao = sc.next();
            modoVerificacao = respostaVerificacao.equalsIgnoreCase("S");
        }

        BlockingQueue<Requisicao> fila = new LinkedBlockingQueue<>();
        HeapSimulada heap = new HeapSimulada(heapKB, estrategia, modoVerificacao);

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
            fila.put(new Requisicao(-1, 0)); // id=-1 sinaliza fim
        }
        for (Thread t : alocadores) t.join();

        long fim = System.currentTimeMillis();
        heap.imprimirResultado(fim - inicio);
        
        try (Scanner sc = new Scanner(System.in)) {
        System.out.print("\nDeseja imprimir o estado final da heap? (S/N): ");
        String resposta = sc.next();
        if (resposta.equalsIgnoreCase("S")) {
        // 2. Se o usuário disser sim, chama o método público na heap.
        heap.imprimirEstadoHeap("ESTADO FINAL DA HEAP");
        }
    }

    System.out.println("\nSimulação concluída.");
    }
}
