import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class Main {
	public static void main(String[] args) {
		setLAF();
		MainFrame mf = new MainFrame();
		mf.setVisible(true);
		mf.startGameLoop();
	}

	public static void setLAF() {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class MainFrame extends JFrame implements Runnable {
	private Thread th = null;

	MainFrame() {
		super("Minesweeper");
		changeTitlePanel();

		setBounds(50, 50, 900, 490);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	void changeMainPanel() {
		getContentPane().removeAll();
		MainPanel mp = new MainPanel();
		add(mp);
		validate();
		repaint();
	}

	void changeTitlePanel() {
		getContentPane().removeAll();
		TitlePanel tp = new TitlePanel();		
		add(tp);
		validate();
		repaint();
	}

	public synchronized void startGameLoop(){
		if ( th == null ) {
			th = new Thread(this);
			th.start();
		}
	}

	public synchronized void stopGameLoop(){
		if ( th != null ) {
			th = null;
		}
	}

	public void run(){
		while(th != null){
			try{
				Thread.sleep(25);
				repaint();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}

class TitlePanel extends JPanel implements ActionListener {
	TitlePanel() {
		setLayout(null);

		Font font = new Font("Courier New", Font.PLAIN, 20);
		JButton btn = new JButton("Game start");
		JButton exb = new JButton("Exit");

		btn.addActionListener(this);
		btn.setBounds(225, 305, 200, 45);
		btn.setFont(font);

		exb.addActionListener(e -> System.exit(0));
		exb.setBounds(460, 305, 200, 45);
		exb.setFont(font);

		add(btn);
		add(exb);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		MainFrame mf = (MainFrame) this.getTopLevelAncestor();
		mf.changeMainPanel();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Font font = new Font("Courier New", Font.PLAIN, 120);
		g.setColor(new Color(240, 240, 240));
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(new Color(40, 40, 40));
		g.setFont(font);
		g.drawString("Minesweeper", 45, 220);
	}
}

class MainPanel extends JPanel implements ActionListener, MouseListener {
	Game game;

	MainPanel() {
		game = new Game();

		addMouseListener(this);
		setLayout(null);
		JButton btn = new JButton("Back to title");
		btn.addActionListener(this);
		btn.setBounds(600, 365, 250, 45);
		btn.setFont(new Font("Courier New", Font.PLAIN, 20));
		add(btn);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		game.gameState = game.STATE_TITLE;
		MainFrame mf = (MainFrame) this.getTopLevelAncestor();
		mf.changeTitlePanel();
	}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {
		game.judgement(e);	
	}
	@Override
	public void mouseReleased(MouseEvent e) {}	
	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(new Color(200, 200, 200));		
		g.fillRect(0, 0, getWidth(), getHeight());
		game.drawCurrentField(g);
		game.drawGameStatus(g);
	}
}

class Game {
	public static final int STATE_TITLE = 0;
	public static final int STATE_PLAY = 1;
	public static final int STATE_GAME_OVER = 2;
	public static final int STATE_GAME_CLEAR = 3;

	private static final int FIELD_WIDTH = 9;
	private static final int FIELD_HEIGHT = 9;
	private static final int BOARD_WIDTH = FIELD_WIDTH + 2;
	private static final int BOARD_HEIGHT = FIELD_HEIGHT + 2;

	private static final int SQUARE_SIZE = 41;
	private static final int FLAG_WIDTH = (SQUARE_SIZE / 5) * 3;
	private static final int FLAG_HEIGHT = (SQUARE_SIZE / 5) * 2;
	private static final int BOMB_RADIUS = SQUARE_SIZE / 4;

	private static final int MARGIN_LEFT = 150;
	private static final int MARGIN_TOP = 0;
	private static final int MARGIN_RIGHT = 80;

	private static final Color BACKGROUND = new Color(200, 200, 200);
	private static final Color CHARACTER = new Color(40, 40, 40);
	private static final Color NORMAL_SQUARE = new Color(128, 128, 128);
	private static final Color OPENED_SQUARE = new Color(222, 222, 222);
	private static final Color BOMB_SQUARE = new Color(255, 50, 50);
	private static final Color BOMB_COLOR = new Color(20, 20, 20);

	private static final Color[] NUMBER_COLORS = {
		new Color(0, 0, 0),
		new Color(0, 51, 255),
		new Color(0, 153, 51),
		new Color(255, 0, 51),
		new Color(0, 51, 102),
		new Color(102, 0, 51),
		new Color(51, 102, 102),
		new Color(0, 0, 51),
		new Color(51, 0, 0),
		new Color(0, 0, 0)
	};
	
	private static final Font COURIER_BOLD_S = new Font("Courier New", Font.BOLD, 25);
	private static final Font ARIAL_BOLD_B = new Font("ARIAL", Font.BOLD, 75);
	private static final Font ARIAL_BOLD_S = new Font("ARIAL", Font.BOLD, 25);

	private static final int SAFE = 0;
	private static final int BOMB = 1;

	private static final int NONE = 0;
	private static final int FLAGGED = 1;
	private static final int OPENED = 2;

	private static Timer timer;
	private static GameTimer gt;
	private static long time;

	public int gameState;
	private int[][] field = new int[BOARD_HEIGHT][BOARD_WIDTH];
	private int[][] bombsCount = new int[BOARD_HEIGHT][BOARD_WIDTH];
	private int[][] fieldStatus = new int[BOARD_HEIGHT][BOARD_WIDTH];
	private int bombs;
	private int opens;
	private int flags;

	Game() {
		initGame();
	}

	void initGame() {
		gameState = STATE_PLAY;
		timer = new Timer();
		gt = new GameTimer();
		time = 0;
		timer.scheduleAtFixedRate(gt, 1000, 1000);
		initFieldStatus();
	}

	void setBombs(int x, int y) {
		bombs = 0;
		Random rand = new Random();
		for (int i = 0; i < BOARD_HEIGHT; i++) {
			for (int j = 0; j < BOARD_WIDTH; j++) {
				if (i == 0 || j == 0 || i == BOARD_HEIGHT - 1 
					|| j == BOARD_WIDTH - 1 || (i == y && j == x)) {
					field[i][j] = SAFE;
					continue;
				}
				int num = rand.nextInt(25);
				if (num <= 3) {
					field[i][j] = BOMB;
				} else {
					field[i][j] = SAFE;
				}
			}
		}
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				field[y-i][x-j] = SAFE;
			}
		}
		countBombs();
		countAroundBombs();
	}

	void initFieldStatus() {
		opens = 0;
		flags = 0;

		for (int i = 0; i < BOARD_HEIGHT; i++) {
			for (int j = 0; j < BOARD_WIDTH; j++) {
				fieldStatus[i][j] = NONE;
			}
		}
	}

	void initBombsCount() {
		for (int i = 0; i < BOARD_HEIGHT; i++) {
			for (int j = 0; j < BOARD_WIDTH; j++) {
				bombsCount[i][j] = 0;
			}
		}
	}

	void checkField() {
		for (int i = 1; i < FIELD_HEIGHT + 1; i++) {
			for (int j = 1; j < FIELD_WIDTH + 1; j++) {
				if (field[i][j] == BOMB && fieldStatus[i][j] == OPENED) {
					gameOver();
				}
			}
		}
	}

	void countOpens() {
		int count = 0;
		for (int i = 1; i <= FIELD_HEIGHT; i++) {
			for (int j = 1; j <= FIELD_WIDTH; j++) {
				if (field[i][j] != BOMB) {
					if (fieldStatus[i][j] == OPENED) {
						count++;
					}				
				}
			}
		}
		opens = count;
	}

	void countBombs() {
		int count = 0;
		for (int i = 1; i <= FIELD_HEIGHT; i++) {
			for (int j = 1; j <= FIELD_WIDTH; j++) {
				if (field[i][j] == BOMB) {
					count++;
				}
			}
		}
		bombs = count;
	}

	void checkClear() {
		if ((FIELD_WIDTH * FIELD_HEIGHT - opens) == bombs) {
			gameState = STATE_GAME_CLEAR;
		}
	}

	int checkGameState() {
		if (gameState == STATE_PLAY) {
			return 0;
		} else if (gameState == STATE_GAME_OVER) {
			return 1;
		} else if (gameState == STATE_GAME_CLEAR) {
			return 2;
		}
		return -1;
	}

	void judgement(MouseEvent e) {
		int xIndex;
		int yIndex;

		if (checkGameState() != 0) {
			xIndex = -1;
			yIndex = -1;	
		} else {
			xIndex = (e.getX() - MARGIN_LEFT) / SQUARE_SIZE;
			yIndex = (e.getY() - MARGIN_TOP) / SQUARE_SIZE;
			
			if (xIndex < 1 || yIndex < 1 || xIndex > 9 || yIndex > 9) {
				xIndex = -1;
				yIndex = -1;
			}
		}

		if (xIndex > 0 && yIndex > 0) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (opens == 0) {
					setBombs(xIndex, yIndex);
				}
				openAround(xIndex, yIndex);
				checkField();
				countOpens();
			}	else if (e.getButton() == MouseEvent.BUTTON3) {
				if (opens != 0) {
					if (fieldStatus[yIndex][xIndex] == NONE) {
						putFlag(xIndex, yIndex);			
					} else if(fieldStatus[yIndex][xIndex] == FLAGGED) {
						takeFlag(xIndex, yIndex);
					}				
				}
			}
		}
		checkClear();
	}

	void putFlag(int x, int y) {
		fieldStatus[y][x] = FLAGGED;
		flags++;
	}

	void takeFlag(int x, int y) {
		fieldStatus[y][x] = NONE;
		flags--;
	}

	void openAround(int x, int y) {
		if (bombsCount[y][x] == 0) {
			if (fieldStatus[y][x] != OPENED) {
				fieldStatus[y][x] = OPENED;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if (i == 0 && j == 0) {
							continue;
						}
						if (i + y > 0 && j + x > 0 && i + y <= FIELD_HEIGHT && j + x <= FIELD_WIDTH) {
							openAround(x + j, y + i);
						}
					}
				}				
			}
			return;
		} else {
			if (fieldStatus[y][x] != FLAGGED) {
				fieldStatus[y][x] = OPENED;
			}
			return;
		}
	}

	void gameOver() {
		gameState = STATE_GAME_OVER;
	}

	void countAroundBombs() {
		initBombsCount();
		for (int i = 1; i <= FIELD_HEIGHT; i++) {
			for (int j = 1; j <= FIELD_WIDTH; j++) {
				int count = 0;

				for (int k = -1; k <= 1; k++) {
					if (field[i][j] == BOMB) {
						count = 9;
						break;
					}
					for (int l = -1; l <= 1; l++) {
						if (field[i-k][j-l] == BOMB) {
							count++;
						}
					}
				}
				bombsCount[i][j] = count;		
			}
		}
	}

	void drawGameStatus(Graphics g) {
		g.setColor(OPENED_SQUARE);	
		Graphics2D g2 = (Graphics2D)g;
		BasicStroke bs = new BasicStroke(3);
		g.setFont(COURIER_BOLD_S);
		FontMetrics fm = g.getFontMetrics();
		g2.setStroke(bs);
		g.fillRect(MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT, MARGIN_TOP + SQUARE_SIZE, 200, 170);
		g.setColor(new Color(100, 100, 100));
		g.drawRect(MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT, MARGIN_TOP + SQUARE_SIZE, 200, 170);
		g.setFont(COURIER_BOLD_S);
		g.drawString("Mines :", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 25, MARGIN_TOP + SQUARE_SIZE + 40);
		g.drawString(bombs + "", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 170 - fm.stringWidth(bombs + ""), MARGIN_TOP + SQUARE_SIZE + 40);
		g.drawString("Flags :", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 25, MARGIN_TOP + SQUARE_SIZE + 70);
		g.drawString(flags + "", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 170 - fm.stringWidth(flags + ""), MARGIN_TOP + SQUARE_SIZE + 70);
		g.drawString("------------", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 10, MARGIN_TOP + SQUARE_SIZE + 100);
		g.drawString("time", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 110, MARGIN_TOP + SQUARE_SIZE + 120);
		g.drawString(time + "", MARGIN_LEFT + FIELD_WIDTH * SQUARE_SIZE + MARGIN_RIGHT + 170 - fm.stringWidth(time + ""), MARGIN_TOP + SQUARE_SIZE + 150);

		if (checkGameState() == 1) {
			g.setFont(ARIAL_BOLD_B);
			g.setColor(CHARACTER);
			g.drawString("GAME OVER", 150, 250);
		} else if (checkGameState() == 2) {
			g.setFont(ARIAL_BOLD_B);
			g.setColor(CHARACTER);
			g.drawString("GAME CLEAR", 150, 250);			
		} 
	}

	void drawCurrentField(Graphics g) {
		for (int i = 1; i <= FIELD_HEIGHT; i++) {
			for (int j = 1; j <= FIELD_WIDTH; j++) {
				if (fieldStatus[i][j] == NONE) {
					g.setColor(NORMAL_SQUARE);
					g.fillRect(MARGIN_LEFT + j * SQUARE_SIZE, MARGIN_TOP + i * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
				} else if (fieldStatus[i][j] == OPENED) {
					g.setColor(OPENED_SQUARE);
					g.fillRect(MARGIN_LEFT + j * SQUARE_SIZE, MARGIN_TOP + i * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
					if (bombsCount[i][j] != 0 && bombsCount[i][j] != 9) {
						g.setColor(NUMBER_COLORS[bombsCount[i][j]]);
						g.setFont(ARIAL_BOLD_S);
						g.drawString(bombsCount[i][j] + "", MARGIN_LEFT + j * SQUARE_SIZE + 15, MARGIN_TOP + i * SQUARE_SIZE + 30);
					} else if (bombsCount[i][j] == 9) {
						g.setColor(BOMB_SQUARE);
						g.fillRect(MARGIN_LEFT + j * SQUARE_SIZE, MARGIN_TOP + i * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
						g.setColor(BOMB_COLOR);
						g.fillOval(MARGIN_LEFT + j * SQUARE_SIZE + 6, MARGIN_TOP + i * SQUARE_SIZE + 6, BOMB_RADIUS * 3, BOMB_RADIUS * 3);
					}
				} else if (fieldStatus[i][j] == FLAGGED) {
					g.setColor(NORMAL_SQUARE);
					g.fillRect(MARGIN_LEFT + j * SQUARE_SIZE, MARGIN_TOP + i * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
					g.setColor(BOMB_SQUARE);
					g.drawLine(MARGIN_LEFT + j * SQUARE_SIZE + 10, MARGIN_TOP + i * SQUARE_SIZE + FLAG_HEIGHT + 8, MARGIN_LEFT + j * SQUARE_SIZE + 10, MARGIN_TOP + i * SQUARE_SIZE + FLAG_HEIGHT + 18);
					g.fillRect(MARGIN_LEFT + j * SQUARE_SIZE + 10, MARGIN_TOP + i * SQUARE_SIZE + 8, FLAG_WIDTH, FLAG_HEIGHT);
				}
				g.setColor(new Color(40, 40, 40));
				g.drawRect(MARGIN_LEFT + j * SQUARE_SIZE, MARGIN_TOP + i * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);				
			}
		}
	}

	class GameTimer extends TimerTask {
		@Override
		public void run() {
			if (gameState == STATE_PLAY) {
				time++;
			} else {
				timer.cancel();
			}
		}
	}
}