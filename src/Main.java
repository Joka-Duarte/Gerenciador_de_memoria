
// Trabalho Parcial - Simulador de Gerenciador de Memoria Dinasmica
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
        }

        BlockingQueue<Requisicao> fila = new LinkedBlockingQueue<>();
        HeapSimulada heap = new HeapSimulada(heapKB);

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
    }
}
