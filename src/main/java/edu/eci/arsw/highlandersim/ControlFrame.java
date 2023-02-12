package edu.eci.arsw.highlandersim;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JScrollBar;

public class ControlFrame extends JFrame {

    private static final int DEFAULT_IMMORTAL_HEALTH = 100;
    private static final int DEFAULT_DAMAGE_VALUE = 10;

    private JPanel contentPane;

    private List<Immortal> immortals;

    private JTextArea output;
    private JLabel statisticsLabel;
    private JScrollPane scrollPane;
    private JTextField numOfImmortals;
    private final AtomicBoolean lockJefe = new AtomicBoolean(false);

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ControlFrame frame = new ControlFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Create the frame.
     */
    public ControlFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 647, 248);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JToolBar toolBar = new JToolBar();
        contentPane.add(toolBar, BorderLayout.NORTH);

        final JButton btnStart = new JButton("Start");
        JButton btnResume = new JButton("Resume");
        JButton btnPauseAndCheck = new JButton("Pause and check");
        JButton btnStop = new JButton("STOP");
        btnStart.addActionListener(e -> {

            immortals = setupInmortals();
            for (Immortal im : immortals) {
                im.start();
            }

            btnStart.setEnabled(false);
            btnPauseAndCheck.setEnabled(true);
            btnStop.setEnabled(true);
        });
        toolBar.add(btnStart);

        btnPauseAndCheck.setEnabled(false);
        btnPauseAndCheck.addActionListener(e -> {
            try {
                lockJefe.set(true);
                synchronized (lockJefe){
                    if (!Immortal.allDead) {
                        lockJefe.wait();
                    }
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            int sum = 0;
            for (Immortal im : immortals) {
                sum += im.getHealth();
            }
            statisticsLabel.setText("<html>"+immortals.toString()+"<br>Health sum:"+ sum);
            btnResume.setEnabled(true);
            btnPauseAndCheck.setEnabled(false);
            btnStop.setEnabled(false);
        });
        toolBar.add(btnPauseAndCheck);

        btnResume.addActionListener(e -> {
            lockJefe.set(false);
            synchronized (Immortal.lockHilos){
                Immortal.lockHilos.notifyAll();
            }
            btnResume.setEnabled(false);
            btnPauseAndCheck.setEnabled(true);
            btnStop.setEnabled(true);
        });
        btnResume.setEnabled(false);
        toolBar.add(btnResume);

        JLabel lblNumOfImmortals = new JLabel("num. of immortals:");
        toolBar.add(lblNumOfImmortals);

        numOfImmortals = new JTextField();
        numOfImmortals.setText("3");
        toolBar.add(numOfImmortals);
        numOfImmortals.setColumns(10);

        btnStop.setForeground(Color.RED);
        toolBar.add(btnStop);
        btnStop.addActionListener(e -> {
            for(Immortal im : immortals) {
                if(im.isAlive()) {
                    synchronized (im) {
                        im.stopImmortal();
                    }
                }
            }
            Immortal.threadDead.set(0);
            immortals = new ArrayList<>();
            TextAreaUpdateReportCallback updateCallback = new TextAreaUpdateReportCallback(output, scrollPane);
            updateCallback.processReport("Se termino la simulación");
            btnStart.setEnabled(true);
            btnPauseAndCheck.setEnabled(false);
            btnResume.setEnabled(false);
            btnStop.setEnabled(false);
        });

        scrollPane = new JScrollPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);

        output = new JTextArea();
        output.setEditable(false);
        scrollPane.setViewportView(output);
        
        
        statisticsLabel = new JLabel("Immortals total health:");
        contentPane.add(statisticsLabel, BorderLayout.SOUTH);

    }

    public List<Immortal> setupInmortals() {

        ImmortalUpdateReportCallback ucb=new TextAreaUpdateReportCallback(output,scrollPane);
        
        try {
            int ni = Integer.parseInt(numOfImmortals.getText());

            List<Immortal> il = new LinkedList<>();

            for (int i = 0; i < ni; i++) {
                Immortal i1 = new Immortal("im" + i, il, DEFAULT_IMMORTAL_HEALTH, DEFAULT_DAMAGE_VALUE,ucb, lockJefe);
                il.add(i1);
            }
            return il;
        } catch (NumberFormatException e) {
            JOptionPane.showConfirmDialog(null, "Número inválido.");
            return null;
        }

    }

}

class TextAreaUpdateReportCallback implements ImmortalUpdateReportCallback{

    JTextArea ta;
    JScrollPane jsp;

    public TextAreaUpdateReportCallback(JTextArea ta,JScrollPane jsp) {
        this.ta = ta;
        this.jsp=jsp;
    }       
    
    @Override
    public void processReport(String report) {
        ta.append(report);

        //move scrollbar to the bottom
        javax.swing.SwingUtilities.invokeLater(() -> {
            JScrollBar bar = jsp.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}