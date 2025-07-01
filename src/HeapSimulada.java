
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// Corpo da Heap
public class HeapSimulada {
    private final int[] heap; // Array de inteiros que representa a memória da heap.
    private final Queue<Requisicao> fifo = new LinkedList<>(); // Fila para gerenciar as requisições alocadas (ordem de chegada).
    private final ReentrantLock lock = new ReentrantLock(); // Objeto de lock para sincronização.

    private int totalAlocadas = 0; // Contador de requisições alocadas com sucesso.
    private long totalBytesAlocados = 0; // Contador do total de bytes alocados.
    private int totalRemovidas = 0; // Contador de variáveis removidas pela política FIFO.
    private int totalCompactacoes = 0; // Contador de quantas vezes a compactação foi chamada.

    // Constrututor da Heap
    public HeapSimulada(int tamanhoKB) {
        this.heap = new int[tamanhoKB * 1024 / 4]; // 1 inteiro = 4 bytes
    }

    // Chamada para Alocação de Requisições
    public boolean alocar(Requisicao r) {
        lock.lock(); // Adquire o lock antes de modificar a heap
        try {
            int inicio = buscarEspacoContiguo(r.blocosNecessarios); // Tenta encontrar espaço
            //novo
            if (inicio == -1) { // Se não encontrou espaço na primeira tentativa
            System.out.println("\n>>>>> ESPAÇO INSUFICIENTE. INICIANDO LIBERAÇÃO E COMPACTAÇÃO <<<<<");
            imprimirEstadoHeap("ESTADO DA HEAP ANTES"); // <-- AQUI USAMOS A FERRAMENTA
        liberarMemoria(); // Tenta liberar memória (baseado na FIFO)
        compactar();      // Compacta a memória para juntar blocos livres


        imprimirEstadoHeap("ESTADO DA HEAP DEPOIS"); // <-- AQUI USAMOS DE NOVO

        inicio = buscarEspacoContiguo(r.blocosNecessarios); // Tenta encontrar espaço novamente
        if (inicio == -1) {
            System.out.println(">>>>> ALOCAÇÃO FALHOU MESMO APÓS COMPACTAÇÃO <<<<<");
            return false; // Se ainda não há espaço, a alocação falha
        }
    }
            //antigo
            /*if (inicio == -1) { // Se não encontrou espaço na primeira tentativa            
                liberarMemoria(); // Tenta liberar memória (baseado na FIFO)
                compactar();      // Compacta a memória para juntar blocos livres
                inicio = buscarEspacoContiguo(r.blocosNecessarios); // Tenta encontrar espaço novamente
                if (inicio == -1) return false; // Se ainda não há espaço, a alocação falha
            }*/
            // Se encontrou espaço, aloca os blocos
            for (int i = 0; i < r.blocosNecessarios; i++) {
                heap[inicio + i] = r.id; // Marca os blocos da heap com o ID da requisição
            }
            fifo.add(r); // Adiciona a requisição à fila FIFO
            totalAlocadas++; // Incrementa o contador de alocações
            totalBytesAlocados += r.tamanhoBytes; // Incrementa o total de bytes alocados
            return true; // Alocação bem-sucedida
        } finally {
            lock.unlock(); // Libera o lock, mesmo que ocorra uma exceção
        }
    }

    /* Metodo First Fit
private int buscarEspacoContiguo(int blocos) {
    int livres = 0, inicio = -1;

    for (int i = 0; i < heap.length; i++) {
        if (heap[i] == 0) { // Se o bloco está livre
            if (livres == 0) inicio = i; // Marca o início do bloco livre
            livres++; // Incrementa o contador de blocos livres contíguos
            if (livres == blocos) return inicio; // Se encontrou espaço suficiente, retorna o início
        } else {
            livres = 0; // Reseta o contador se encontrar um bloco ocupado
        }
    }
    return -1; // Não encontrou espaço
}
*/
    
    // Metodo Worst Fit
private int buscarEspacoContiguo(int blocos) {
    int piorInicio = -1; // Início do maior bloco encontrado até agora
    int piorTamanho = -1; // Tamanho do maior bloco encontrado

    int i = 0;
    while (i < heap.length) {
        if (heap[i] == 0) { // Se encontrou um bloco livre
            int inicio = i;
            int tamanho = 0;

            // Conta o tamanho do bloco livre atual
            while (i < heap.length && heap[i] == 0) {
                tamanho++;
                i++;
            }

            // Verifica se é suficiente e maior que o "pior" anterior
            if (tamanho >= blocos && tamanho > piorTamanho) {
                piorInicio = inicio;
                piorTamanho = tamanho;
            }
        } else {
            i++; // Pula blocos ocupados
        }
    }
    return piorInicio; // Retorna o início do maior bloco adequado
}

    
    /* Metodo Best Fit
private int buscarEspacoContiguo(int blocos) {
    int melhorInicio = -1; // Início do bloco mais "justo" encontrado
    int melhorTamanho = Integer.MAX_VALUE; // Tamanho do bloco mais "justo"

    int i = 0;
    while (i < heap.length) {
        if (heap[i] == 0) { // Se encontrou um bloco livre
            int inicio = i;
            int tamanho = 0;

            // Conta o tamanho do espaço livre atual
            while (i < heap.length && heap[i] == 0) {
                tamanho++;
                i++;
            }

            // Verifica se é suficiente E se é menor que o "melhor" anterior (mas ainda cabendo)
            if (tamanho >= blocos && tamanho < melhorTamanho) {
                melhorInicio = inicio;
                melhorTamanho = tamanho;
            }
        } else {
            i++; // Pula blocos ocupados
        }
    }
    return melhorInicio; // Retorna o início do bloco que melhor se encaixa
}
*/
    private void liberarMemoria() {
        int blocosParaLiberar = (int) (heap.length * 0.3); // Define uma meta: liberar 30% do total de blocos da heap
        int liberados = 0; // Contador de blocos efetivamente liberados nesta chamada

        // Continua enquanto a fila FIFO não estiver vazia E a meta de liberação não for atingida
        while (!fifo.isEmpty() && liberados < blocosParaLiberar) {
            Requisicao r = fifo.poll(); // Remove a requisição mais antiga da fila (FIFO)
            // Itera por toda a heap para encontrar e zerar os blocos associados a esta requisição
            for (int i = 0; i < heap.length; i++) {
                if (heap[i] == r.id) { // Se o bloco pertence à requisição 'r'
                    heap[i] = 0;    // Marca o bloco como livre
                    liberados++;    // Incrementa o contador de blocos liberados
                }
            }
            totalRemovidas++; // Incrementa o contador total de variáveis removidas
        }
    }

    private void compactar() {
        int[] novaHeap = new int[heap.length]; // Cria um novo array temporário do mesmo tamanho da heap
        int indice = 0; // Índice para a novaHeap

        // Copia todos os blocos ocupados da heap original para a novaHeap, sequencialmente
        for (int valor : heap) { // Itera sobre cada bloco da heap antiga
            if (valor != 0) { // Se o bloco está ocupado
                novaHeap[indice++] = valor; // Copia para a novaHeap e avança o índice
            }
        }

        // Copia o conteúdo da novaHeap (que agora está compactada) de volta para a heap original
        System.arraycopy(novaHeap, 0, heap, 0, heap.length);
        totalCompactacoes++; // Incrementa o contador de compactações
    }
    
    // Impressão
    public void imprimirResultado(long tempoTotalMs) {
        System.out.println("\n----- RESULTADOS -----");
        System.out.println("Total de requisicoes atendidas: " + totalAlocadas);
        System.out.println("Tamanho medio das variaveis alocadas: " + (totalAlocadas > 0 ? (totalBytesAlocados / totalAlocadas) : 0) + " bytes");
        System.out.println("Total de variaveis removidas (FIFO): " + totalRemovidas);
        System.out.println("Chamadas ao algoritmo de compactacao: " + totalCompactacoes);
        System.out.println("Tempo total de execucao: " + tempoTotalMs + " ms");
    }
    
 /**
 * Imprime o estado atual do array da heap no console para fins de depuração.
 * @param titulo Um título para a impressão, como "Antes da Compactação".
 */
private void imprimirEstadoHeap(String titulo) {
    System.out.println("\n----- " + titulo + " -----");
    // Para não poluir o console, vamos imprimir 50 blocos por linha
    for (int i = 0; i < heap.length; i++) {
        System.out.print(heap[i] + " ");
        if ((i + 1) % 50 == 0 || i == heap.length - 1) {
            System.out.println();
        }
    }
    System.out.println("----------------------------------------");
}

}
