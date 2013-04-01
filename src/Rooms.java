import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import org.eclipse.swt.custom.StyledText;

public class Rooms extends Composite {
	private Text text;
	private final static String EXCHANGE_NAME = "MyZone";
	private StyledText txt;
	private String roomname;
	private String serverip;
	private Channel channel;
	private Connection connection;
	private Thread thread;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public Rooms(Composite parent, int style, final String name, String ip, final String selfname) {
		super(parent, style);
		roomname = name;
		serverip = ip;
		
		text = new Text(this, SWT.BORDER);
		text.setBounds(10, 10, 306, 25);
		
		Button button = new Button(this, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (text.getText() != "") {
					String message = selfname + ":" + text.getText();
					SendMessage(message, name);
					text.setText("");
				}
			}
		});
		button.setText("send");
		button.setBounds(337, 10, 80, 25);
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setBounds(10, 41, 405, 201);
		
		txt = new StyledText(scrolledComposite, SWT.BORDER);
		txt.setEditable(false);
		scrolledComposite.setContent(txt);
		scrolledComposite.setMinSize(txt.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnNewButton = new Button(this, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Leave();
			}
		});
		btnNewButton.setBounds(337, 248, 80, 25);
		btnNewButton.setText("Leave");
		
		thread = new Thread(new Runnable(){
			public void run() {
				startServer(name);
			}
		});
		thread.start();
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
	
	protected void SendMessage(String message, String roomname){
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(serverip);
		Connection connection;
		try {
			connection = factory.newConnection();
			Channel channel = connection.createChannel();
			channel.exchangeDeclare(EXCHANGE_NAME, "direct");
			channel.basicPublish(EXCHANGE_NAME, roomname, null, message.getBytes());
			
			channel.close();
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void startServer(String roomname) {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(serverip);
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			
			String queuename = channel.queueDeclare().getQueue();
			channel.exchangeDeclare(EXCHANGE_NAME, "direct");
			channel.queueBind(queuename, EXCHANGE_NAME, roomname);
			
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queuename, true, consumer);
			
			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());
				addMessage(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ShutdownSignalException e) {
			//e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void Leave() {
		try {
			if (channel.isOpen()) {
				channel.close();
				connection.close();
			}
			thread.interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected String getRoom(){
		return roomname;
	}
	
	protected void addMessage(final String message) {
		Display.getDefault().syncExec(new Runnable(){
			public void run() {
				txt.setText(txt.getText() + message + "\n");
			}
		});
	}
}
