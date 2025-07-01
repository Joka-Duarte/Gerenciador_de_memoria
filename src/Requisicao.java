
// Classe Requisição
public class Requisicao {
    public final int id; // Identificador único da requisição
    public final int tamanhoBytes; // Quantidade de blocos de memória (inteiros de 4 bytes) que esta requisição precisa
    public final int blocosNecessarios; // Tamanho total em bytes da requisição

    // Construtor
    public Requisicao(int id, int tamanhoBytes) {
        this.id = id;
        this.tamanhoBytes = tamanhoBytes;
        this.blocosNecessarios = (int) Math.ceil(tamanhoBytes / 4.0); // Calcula blocos, arredondando para cima
    }
}
