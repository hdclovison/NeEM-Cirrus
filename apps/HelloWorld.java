package apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;

public class HelloWorld extends Thread{
	
	//Atributos
	private MulticastChannel neem;
	private int numMsgEnv; //para diferenciar uma mensagem enviada da outra
	private int numMsgRec; //contador do número de mensagens recebidas
	private long intervalo_envio;	//define a cada quantos milissegundos é enviada uma mensagem para todos os outros
	private char tipo_envio;
	
	//Construtor
	public HelloWorld(MulticastChannel mult_channel, char tipo, long per)
	{
		this.neem = mult_channel;
		this.numMsgEnv = 0;
		this.numMsgRec = 0;
		this.intervalo_envio = per;
		this.tipo_envio = tipo;
		this.neem.setLoopbackMode(false);	//se está em loopback mode, a mensagem enviada pelo ponto não deve ser recebida pelo próprio
		setDaemon(true);
        start();
	}
	
	//Métodos
	public void enviaPeriod() throws IOException, InterruptedException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Info: A cada " + this.intervalo_envio + " ms envia mensagem");
		while (true) 
		{
			this.numMsgEnv++;
			Thread.sleep(this.intervalo_envio);
			String msg = new String("MsgID=" + this.numMsgEnv + "| Hello World " + this.numMsgEnv + " de " + neem.getLocalSocketAddress());
			ByteBuffer mensagem = ByteBuffer.wrap(msg.getBytes());
			neem.write(mensagem);
		}	
	}
	
	public void enviaInput() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Info: Aperte qualquer coisa para iniciar uma rodada de gossiping");
		while (br.readLine() != null) 
		{
			this.numMsgEnv++;
			String msg = new String("MsgID=" + this.numMsgEnv + "| Hello World " + this.numMsgEnv + " de " + neem.getLocalSocketAddress());
			ByteBuffer mensagem = ByteBuffer.wrap(msg.getBytes());
			neem.write(mensagem);
		}
	}
	
	public void envia() throws IOException, InterruptedException
	{
		if (this.tipo_envio == 'p')
			enviaPeriod();
		else if (this.tipo_envio == 't')
			enviaInput();
		else
		{
			System.out.println("Info: O nodo só pode receber mensagens");
			while (true)
			{
				//Não faz nada. Só é utilizado o laço para não fechar o canal.
			}
		}
	}
	
	public void run() //recebe da nuvem
	{
		try
		{
			while (true)
			{
				byte[] b = new byte[65]; 
				ByteBuffer bb = ByteBuffer.wrap(b);
				neem.read(bb);
				this.numMsgRec++;
				System.out.println(this.numMsgRec + ".a Msg recebida: \"" + new String(b));
			} 
		} catch (AsynchronousCloseException ace) {
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 3) //se não possuir argumentos, exibe mensagem na tela e encerra
		{
			System.err.println("Modo de uso: apps.HelloWorld endereço_local tipo_envio t_ms endereço_ponto_1 ... endereço_ponto_n");
			System.err.println("Se tipo_envio = p, envia mensagens a cada t_ms");
			System.err.println("Se tipo_envio = t, envia a cada input do teclado");
			System.err.println("Senão, apenas recebe mensagens.");
			System.exit(1);
		}
		
		try
		{
			MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true)); //instancia o NeEM com a classe Multicast Channel
			
			//registra o bean para o JMX. O acesso se dá pelo jconsole em /usr/java/sdk*/bin
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ProtocolMBean mbean = neem.getProtocolMBean();
            ObjectName name = new ObjectName("net.sf.neem:type=Protocol,id="+mbean.getLocalId());
            mbs.registerMBean(mbean, name);
            
            char tipo = args[1].charAt(0);
            long intervalo = Long.parseLong(args[2]);     
            
			HelloWorld hello = new HelloWorld(neem, tipo, intervalo); //instancia essa classe com o NeEM
			
			System.out.println("Nodo " + neem.getLocalSocketAddress() + " foi iniciado.");
			
			for(int i=3; i<args.length; i++)
			{
				neem.connect(Addresses.parse(args[i], false));//conecta aos outros pontos informados
			}
			
			hello.envia(); //envia mensagem para a nuvem, se for o caso
			
			neem.close(); //fecha o canal
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}

}
