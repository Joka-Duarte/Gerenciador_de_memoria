
// Trabalho Final - Simulador de Gerenciador de Memoria Dinamica
// Integrantes Breno Henrique & João Duarte


import java.util.Scanner;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Cria um único Scanner para ser usado durante toda a execução.
        Scanner sc = new Scanner(System.in);

        // Declaração das variáveis de configuração.
        int heapKB;
        int min;
        int max;
        int total;
        int threads;
        EstrategiaAlocacao estrategia;
        boolean modoVerificacao;

        // Coleta das informações do usuário.
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

        System.out.print("\nAtivar modo de verificação da heap? (S/N): ");
        String respostaVerificacao = sc.next();
        modoVerificacao = respostaVerificacao.equalsIgnoreCase("S");

        // O bloco try-with-resources foi removido para não fechar o Scanner.

        // Configuração e inicialização da simulação.
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

        // Aguarda a conclusão das threads.
        gerador.join();
        for (int i = 0; i < threads; i++) {
            fila.put(new Requisicao(-1, 0)); // Envia a "poison pill"
        }
        for (Thread t : alocadores) t.join();

        long fim = System.currentTimeMillis();
        
        // Exibição dos resultados.
        heap.imprimirResultado(fim - inicio);

        // Pergunta final, usando o mesmo Scanner que ainda está aberto.
        System.out.print("\nDeseja imprimir o estado final da heap? (S/N): ");
        String respostaFinal = sc.next();
        if (respostaFinal.equalsIgnoreCase("S")) {
            heap.imprimirEstadoHeap("ESTADO FINAL DA HEAP");
        }

        sc.close(); // Agora sim, fechamos o Scanner no final de tudo.
        System.out.println("\nSimulação concluída.");
    }
}