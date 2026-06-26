import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

/**
 
 *  - Dual-line display  : expression row (greyed) + large result row
 *  - Correct chaining   : 2 + 3 + 4 = 9  (evaluates left-to-right)
 *  - Memory functions   : MC  MR  M+  M−
 *  - History sidebar    : scrollable, newest-on-top, last 50 entries
 *  - Keyboard support   : digits, + - * / % . Enter Backspace Escape
 *  - Hover / press FX   : buttons brighten on hover, darken on press
 *  - Fixed grid layout  : no panel.remove() hacks; clean 6×4 + footer row
 *  - Smart formatting   : no trailing ".0"; shrinks font for long numbers
 *  - Error handling     : divide-by-zero, sqrt of negative, bad parse
 */
public class CalculatorUI extends JFrame implements ActionListener {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG         = new Color(18,  18,  18);
    private static final Color DISPLAY_BG = new Color(28,  28,  28);
    private static final Color BTN_NUM    = new Color(55,  55,  55);
    private static final Color BTN_OP     = new Color(210, 120,   0);
    private static final Color BTN_FN     = new Color(72,  72,  72);
    private static final Color BTN_CLEAR  = new Color(190,  45,  45);
    private static final Color BTN_EQUAL  = new Color(40,  160,  70);
    private static final Color BTN_MEM    = new Color(45,   90, 170);
    private static final Color FG         = Color.WHITE;
    private static final Color EXPR_FG    = new Color(150, 150, 150);

    // ── Display ───────────────────────────────────────────────────────────────
    private JLabel expressionLabel;
    private JLabel resultLabel;

    // ── History ───────────────────────────────────────────────────────────────
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();

    // ── State ─────────────────────────────────────────────────────────────────
    private String  currentInput = "";
    private double  storedValue  = 0;
    private char    operator     = ' ';
    private boolean justEvaled   = false;
    private double  memory       = 0;

    private final DecimalFormat fmt = new DecimalFormat("#.##########");

    // ── Buttons ───────────────────────────────────────────────────────────────
    private final JButton[] numBtns = new JButton[10];
    private JButton dotBtn, clearBtn, delBtn, signBtn;
    private JButton opAdd, opSub, opMul, opDiv, opPow;
    private JButton sqrtBtn, pctBtn, equalBtn;
    private JButton mcBtn, mrBtn, mPlusBtn, mMinusBtn;

    // ─────────────────────────────────────────────────────────────────────────
    public CalculatorUI() {
        setTitle("Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildDisplay(), BorderLayout.NORTH);
        add(buildButtons(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        setupKeyboard();
        updateDisplay();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Display panel ─────────────────────────────────────────────────────────
    private JPanel buildDisplay() {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(DISPLAY_BG);
        panel.setBorder(new EmptyBorder(14, 16, 12, 16));
        panel.setPreferredSize(new Dimension(320, 100));

        expressionLabel = new JLabel(" ");
        expressionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        expressionLabel.setForeground(EXPR_FG);
        expressionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        resultLabel = new JLabel("0");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        resultLabel.setForeground(FG);
        resultLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(expressionLabel, BorderLayout.NORTH);
        panel.add(resultLabel,     BorderLayout.CENTER);
        return panel;
    }

    // ── Button grid (6 rows × 4 cols) ────────────────────────────────────────
    private JPanel buildButtons() {
        JPanel p = new JPanel(new GridLayout(7, 4, 6, 6));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Row 1 – Memory
        mcBtn     = mkBtn(p, "MC",  BTN_MEM);
        mrBtn     = mkBtn(p, "MR",  BTN_MEM);
        mPlusBtn  = mkBtn(p, "M+",  BTN_MEM);
        mMinusBtn = mkBtn(p, "M−",  BTN_MEM);

        // Row 2 – Clear / Functions
        clearBtn = mkBtn(p, "C",   BTN_CLEAR);
        delBtn   = mkBtn(p, "⌫",   BTN_FN);
        pctBtn   = mkBtn(p, "%",   BTN_FN);
        opDiv    = mkBtn(p, "÷",   BTN_OP);

        // Row 3 – Scientific
        sqrtBtn = mkBtn(p, "√",   BTN_FN);
        opPow   = mkBtn(p, "xʸ",  BTN_FN);
        signBtn = mkBtn(p, "±",   BTN_FN);
        opMul   = mkBtn(p, "×",   BTN_OP);

        // Row 4 – 7 8 9 −
        numBtns[7] = mkBtn(p, "7", BTN_NUM);
        numBtns[8] = mkBtn(p, "8", BTN_NUM);
        numBtns[9] = mkBtn(p, "9", BTN_NUM);
        opSub      = mkBtn(p, "−", BTN_OP);

        // Row 5 – 4 5 6 +
        numBtns[4] = mkBtn(p, "4", BTN_NUM);
        numBtns[5] = mkBtn(p, "5", BTN_NUM);
        numBtns[6] = mkBtn(p, "6", BTN_NUM);
        opAdd      = mkBtn(p, "+", BTN_OP);

        // Row 6 – 1 2 3 =
        numBtns[1] = mkBtn(p, "1", BTN_NUM);
        numBtns[2] = mkBtn(p, "2", BTN_NUM);
        numBtns[3] = mkBtn(p, "3", BTN_NUM);
        equalBtn   = mkBtn(p, "=", BTN_EQUAL);

        // Row 7 – . 0 (two empty fillers so = stays right)
        dotBtn     = mkBtn(p, ".",  BTN_NUM);
        numBtns[0] = mkBtn(p, "0", BTN_NUM);
        addFiller(p);
        addFiller(p);

        return p;
    }

    private JButton mkBtn(JPanel panel, String label, Color bg) {
        JButton b = new JButton(label);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setBackground(bg);
        b.setForeground(FG);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(68, 54));
        Color hover = bg.brighter();
        Color press = bg.darker();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e)  { b.setBackground(hover); }
            public void mouseExited (MouseEvent e)  { b.setBackground(bg);    }
            public void mousePressed(MouseEvent e)  { b.setBackground(press); }
            public void mouseReleased(MouseEvent e) { b.setBackground(hover); }
        });
        b.addActionListener(this);
        panel.add(b);
        return b;
    }

    private void addFiller(JPanel p) {
        JPanel f = new JPanel();
        f.setBackground(BG);
        p.add(f);
    }

    // ── History sidebar ───────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JList<String> list = new JList<>(historyModel);
        list.setBackground(new Color(22, 22, 22));
        list.setForeground(new Color(175, 175, 175));
        list.setFont(new Font("SansSerif", Font.PLAIN, 12));
        list.setFixedCellHeight(26);
        list.setBorder(new EmptyBorder(2, 6, 2, 6));

        JScrollPane scroll = new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel title = new JLabel("  History");
        title.setOpaque(true);
        title.setBackground(DISPLAY_BG);
        title.setForeground(EXPR_FG);
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setPreferredSize(new Dimension(155, 28));
        title.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(new Color(22, 22, 22));
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(48, 48, 48)));
        side.add(title,  BorderLayout.NORTH);
        side.add(scroll, BorderLayout.CENTER);
        side.setPreferredSize(new Dimension(155, 0));
        return side;
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    private void setupKeyboard() {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                int  k = e.getKeyCode();
                if      (c >= '0' && c <= '9') pressDigit(c - '0');
                else if (c == '.')              pressDot();
                else if (c == '+')              pressOperator('+');
                else if (c == '-')              pressOperator('-');
                else if (c == '*')              pressOperator('*');
                else if (c == '/')              pressOperator('/');
                else if (c == '%')              pressPct();
                else if (c == '\n' || c == '=') pressEqual();
                else if (k == KeyEvent.VK_BACK_SPACE) pressDelete();
                else if (k == KeyEvent.VK_ESCAPE)     pressClear();
            }
        });
        setFocusable(true);
    }

    // ── ActionListener dispatch ───────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        for (int i = 0; i < 10; i++)
            if (src == numBtns[i]) { pressDigit(i); return; }

        if (src == dotBtn)    pressDot();
        else if (src == clearBtn)  pressClear();
        else if (src == delBtn)    pressDelete();
        else if (src == signBtn)   pressSign();
        else if (src == pctBtn)    pressPct();
        else if (src == sqrtBtn)   pressSqrt();
        else if (src == equalBtn)  pressEqual();
        else if (src == opAdd)     pressOperator('+');
        else if (src == opSub)     pressOperator('-');
        else if (src == opMul)     pressOperator('*');
        else if (src == opDiv)     pressOperator('/');
        else if (src == opPow)     pressOperator('^');
        else if (src == mcBtn)     memory = 0;
        else if (src == mrBtn)     recallMemory();
        else if (src == mPlusBtn)  memoryOp(+1);
        else if (src == mMinusBtn) memoryOp(-1);
    }

    // ── Input handlers ────────────────────────────────────────────────────────
    private void pressDigit(int d) {
        if (justEvaled) { currentInput = ""; justEvaled = false; }
        if (currentInput.equals("0")) currentInput = "";
        currentInput += d;
        updateDisplay();
    }

    private void pressDot() {
        if (justEvaled) { currentInput = "0"; justEvaled = false; }
        if (!currentInput.contains("."))
            currentInput = currentInput.isEmpty() ? "0." : currentInput + ".";
        updateDisplay();
    }

    private void pressOperator(char op) {
        // Chain: evaluate pending operation first, then queue new operator
        if (!currentInput.isEmpty() && operator != ' ') {
            evaluate(false);
        } else if (!currentInput.isEmpty()) {
            storedValue = Double.parseDouble(currentInput);
        }
        currentInput = "";
        operator     = op;
        justEvaled   = false;
        updateDisplay();
    }

    private void pressEqual() {
        if (currentInput.isEmpty() || operator == ' ') return;
        evaluate(true);
    }

    private void pressClear() {
        currentInput = "";
        storedValue  = 0;
        operator     = ' ';
        justEvaled   = false;
        expressionLabel.setText(" ");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        resultLabel.setText("0");
    }

    private void pressDelete() {
        if (justEvaled) { pressClear(); return; }
        if (!currentInput.isEmpty())
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        updateDisplay();
    }

    private void pressSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) return;
        double v = Double.parseDouble(currentInput) * -1;
        currentInput = fmt.format(v);
        updateDisplay();
    }

    private void pressPct() {
        if (currentInput.isEmpty()) return;
        double v = Double.parseDouble(currentInput) / 100.0;
        currentInput = fmt.format(v);
        updateDisplay();
    }

    private void pressSqrt() {
        if (currentInput.isEmpty()) return;
        double v = Double.parseDouble(currentInput);
        if (v < 0) { showError("√ of negative"); return; }
        String expr = "√(" + currentInput + ")";
        currentInput = fmt.format(Math.sqrt(v));
        addHistory(expr + " = " + currentInput);
        justEvaled = true;
        expressionLabel.setText(expr + " =");
        resultLabel.setFont(sizedFont(currentInput));
        resultLabel.setText(currentInput);
    }

    private void recallMemory() {
        if (justEvaled) { currentInput = ""; justEvaled = false; }
        currentInput = fmt.format(memory);
        updateDisplay();
    }

    private void memoryOp(int sign) {
        double v = currentInput.isEmpty() ? storedValue : Double.parseDouble(currentInput);
        memory += sign * v;
    }

    // ── Evaluation ────────────────────────────────────────────────────────────
    private void evaluate(boolean fromEqual) {
        double right;
        try { right = Double.parseDouble(currentInput); }
        catch (NumberFormatException ex) { showError("Error"); return; }

        String exprStr = fmt.format(storedValue) + " " + opSym(operator)
                + " " + fmt.format(right);
        double result;
        switch (operator) {
            case '+': result = storedValue + right; break;
            case '-': result = storedValue - right; break;
            case '*': result = storedValue * right; break;
            case '/':
                if (right == 0) { showError("÷ 0 undefined"); return; }
                result = storedValue / right; break;
            case '^': result = Math.pow(storedValue, right); break;
            default: return;
        }

        String resultStr = fmt.format(result);
        addHistory(exprStr + " = " + resultStr);

        expressionLabel.setText(exprStr + " =");
        resultLabel.setFont(sizedFont(resultStr));
        resultLabel.setText(resultStr);

        storedValue  = result;
        currentInput = resultStr;
        operator     = ' ';
        justEvaled   = fromEqual;
    }

    // ── Display helpers ───────────────────────────────────────────────────────
    private void updateDisplay() {
        String show = currentInput.isEmpty() ? "0" : currentInput;
        resultLabel.setFont(sizedFont(show));
        resultLabel.setText(show);

        if (operator != ' ') {
            expressionLabel.setText(fmt.format(storedValue) + " " + opSym(operator));
        } else if (!justEvaled) {
            expressionLabel.setText(" ");
        }
    }

    private Font sizedFont(String s) {
        int size = s.length() > 14 ? 18 : s.length() > 10 ? 26 : 38;
        return new Font("SansSerif", Font.BOLD, size);
    }

    private void showError(String msg) {
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        resultLabel.setText(msg);
        expressionLabel.setText(" ");
        currentInput = ""; operator = ' '; storedValue = 0; justEvaled = false;
    }

    private void addHistory(String entry) {
        historyModel.add(0, entry);
        if (historyModel.size() > 50) historyModel.remove(historyModel.size() - 1);
    }

    private String opSym(char op) {
        switch (op) {
            case '+': return "+";
            case '-': return "−";
            case '*': return "×";
            case '/': return "÷";
            case '^': return "^";
            default:  return "";
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(CalculatorUI::new);
    }
}
