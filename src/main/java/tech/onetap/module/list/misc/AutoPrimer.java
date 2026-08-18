package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "Auto Primer", moduleDesc = "Автоматически решает примеры из чат-игры", moduleCategory = ModuleCategory.MISC)
public class AutoPrimer extends Module {

    private static final Pattern TASK_LINE_PATTERN = Pattern.compile("(?iu)пример\\s*[:=]?\\s*(.+)");
    private static final Pattern STOP_PATTERN = Pattern.compile("(?iu)(получит|получишь|награда|приз|ответ|решил|решала)");

    private final SliderSetting delay = new SliderSetting("Задержка", 1, 0, 20, 1);

    private String pendingAnswer;
    private String lastTaskKey;
    private long lastTaskTime;
    private int delayTicks;

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) {
            clearState();
            return;
        }

        if (e.getType() != EventPacket.Type.RECEIVE) return;
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String message = packet.content().getString();
        if (message == null || message.isBlank()) return;

        if (isGameFinishedMessage(message)) {
            clearPendingAnswer();
            return;
        }

        if (!isTaskMessage(message)) return;
        String expression = extractExpression(message);
        if (expression == null) return;

        String taskKey = expression;
        long now = System.currentTimeMillis();
        if (taskKey.equals(lastTaskKey) && now - lastTaskTime < 4_000L) return;

        String answer = solve(expression);
        if (answer == null) return;

        pendingAnswer = answer;
        delayTicks = Math.max(0, delay.getIntValue() - 1);
        lastTaskKey = taskKey;
        lastTaskTime = now;
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            clearState();
            return;
        }

        if (pendingAnswer == null) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        mc.getNetworkHandler().sendChatMessage(pendingAnswer);
        clearPendingAnswer();
    }

    @Override
    public void onDisable() {
        clearState();
        super.onDisable();
    }

    private boolean isTaskMessage(String message) {
        String normalized = normalizeText(message);
        return normalized.contains("пример") || normalized.contains("решит");
    }

    private boolean isGameFinishedMessage(String message) {
        String normalized = normalizeText(message);
        return normalized.contains("решилпримерпервым") || normalized.contains("ответбыл");
    }

    private String extractExpression(String message) {
        String[] lines = message.split("\\R");
        for (String line : lines) {
            var matcher = TASK_LINE_PATTERN.matcher(line);
            if (!matcher.find()) continue;

            String expression = matcher.group(1);
            var stopMatcher = STOP_PATTERN.matcher(expression);
            if (stopMatcher.find()) {
                expression = expression.substring(0, stopMatcher.start());
            }

            expression = normalizeExpression(expression);
            if (!expression.isEmpty()) {
                return expression;
            }
        }

        return null;
    }

    private String solve(String expression) {
        try {
            double value = new ExpressionParser(expression).parse();
            if (!Double.isFinite(value)) return null;
            BigDecimal result = BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
            if (result.compareTo(BigDecimal.ZERO) == 0) return "0";
            return result.toPlainString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalizeExpression(String expression) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            switch (ch) {
                case ' ', '\t', '\r', '\n' -> {
                }
                case ',' -> builder.append('.');
                case 'x', 'X', 'х', 'Х', '×' -> builder.append('*');
                case ':', '÷' -> builder.append('/');
                case '√' -> builder.append("sqrt");
                case '∛' -> builder.append("cbrt");
                case '∜' -> builder.append("qdrt");
                case '⁰' -> builder.append("^0");
                case '¹' -> builder.append("^1");
                case '²' -> builder.append("^2");
                case '³' -> builder.append("^3");
                case '⁴' -> builder.append("^4");
                case '⁵' -> builder.append("^5");
                case '⁶' -> builder.append("^6");
                case '⁷' -> builder.append("^7");
                case '⁸' -> builder.append("^8");
                case '⁹' -> builder.append("^9");
                default -> {
                    if (Character.isDigit(ch) || ch == '.' || ch == '(' || ch == ')' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^') {
                        builder.append(ch);
                        continue;
                    }

                    if (Character.isLetter(ch)) {
                        builder.append(Character.toLowerCase(ch));
                        continue;
                    }

                    return "";
                }
            }
        }

        String normalized = builder.toString();
        normalized = normalized.replaceAll("(?<=[\\d)])(?=\\()", "*");
        normalized = normalized.replaceAll("(?<=[\\d)])(?=(sqrt|cbrt|qdrt))", "*");
        normalized = normalized.replaceAll("(?<=\\))(?=\\d)", "*");
        normalized = normalized.replaceAll("(?<=\\d)(?=(sqrt|cbrt|qdrt))", "*");
        return normalized;
    }

    private String normalizeText(String message) {
        return message.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("\\s+", "");
    }

    private void clearPendingAnswer() {
        pendingAnswer = null;
        delayTicks = 0;
    }

    private void clearState() {
        clearPendingAnswer();
        lastTaskKey = null;
        lastTaskTime = 0L;
    }

    private static class ExpressionParser {
        private final String expression;
        private int pos = -1;
        private int ch;

        private ExpressionParser(String expression) {
            this.expression = expression;
        }

        private double parse() {
            nextChar();
            double value = parseExpression();
            if (pos < expression.length()) {
                throw new RuntimeException();
            }
            return value;
        }

        private void nextChar() {
            ch = ++pos < expression.length() ? expression.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                if (eat('+')) {
                    value += parseTerm();
                    continue;
                }

                if (eat('-')) {
                    value -= parseTerm();
                    continue;
                }

                return value;
            }
        }

        private double parseTerm() {
            double value = parsePower();
            while (true) {
                if (eat('*')) {
                    value *= parsePower();
                    continue;
                }

                if (eat('/')) {
                    value /= parsePower();
                    continue;
                }

                return value;
            }
        }

        private double parsePower() {
            double value = parseUnary();
            if (eat('^')) {
                value = Math.pow(value, parsePower());
            }
            return value;
        }

        private double parseUnary() {
            if (eat('+')) return parseUnary();
            if (eat('-')) return -parseUnary();

            if (matchFunction("sqrt")) {
                return Math.sqrt(parseUnary());
            }

            if (matchFunction("cbrt")) {
                return Math.cbrt(parseUnary());
            }

            if (matchFunction("qdrt")) {
                return Math.pow(parseUnary(), 0.25D);
            }

            return parsePrimary();
        }

        private double parsePrimary() {
            if (eat('(')) {
                double value = parseExpression();
                if (!eat(')')) {
                    throw new RuntimeException();
                }
                return value;
            }

            int start = pos;
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                nextChar();
            }

            if (start == pos) {
                throw new RuntimeException();
            }

            return Double.parseDouble(expression.substring(start, pos));
        }

        private boolean matchFunction(String function) {
            if (!expression.regionMatches(pos, function, 0, function.length())) return false;
            pos += function.length();
            ch = pos < expression.length() ? expression.charAt(pos) : -1;
            return true;
        }
    }
}
