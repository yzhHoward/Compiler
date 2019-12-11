import SyntaxAnalyzer.SyntaxAnalyzer;
import WordAnalyzer.WordAnalyzer;
import WordAnalyzer.WordException;
import WordAnalyzer.WordSymbol;

public class cc0 {

    private static void wordAnalyze(String source) {
        WordAnalyzer wordAnalyzer = new WordAnalyzer(source);
        while (true) {
            try {
                if (wordAnalyzer.getsym() == WordSymbol.Unknown || wordAnalyzer.symbol == WordSymbol.EOF) break;
                System.out.println(String.format("%-16d%-16s%-16s%-16s", wordAnalyzer.count, wordAnalyzer.getType(), wordAnalyzer.symbol, wordAnalyzer.token));
            } catch (WordException e) {
                System.out.println(e.getMessage() + " at " + wordAnalyzer.lineOffset + ":" + (wordAnalyzer.wordOffset - wordAnalyzer.token.length() + 1) + " word: " + wordAnalyzer.token);
                break;
            }
        }
    }

    private static void syntaxAnalyze(String source, boolean text) {
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer(new WordAnalyzer(source));
        syntaxAnalyzer.start(text);
    }

    private static void syntaxAnalyze(String source, String outputPath, boolean text) {
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer(new WordAnalyzer(source), outputPath);
        syntaxAnalyzer.start(text);
    }

    public static void main(String[] args) {
        String source = "./1.c";
        String outputPath = "./1.txt";
        boolean text = true;
        boolean output = false;
        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-s":
                    text = true;
                    break;
                case "-c":
                    text = false;
                    break;
                case "-h":
                    System.out.println("  -s        将输入的 c0 源代码翻译为文本汇编文件\n" +
                            "  -c        将输入的 c0 源代码翻译为二进制目标文件\n" +
                            "  -h        显示关于编译器使用的帮助\n" +
                            "  -o file   输出到指定的文件 file");
                    break;
                case "-o":
                    if (i + 1 < args.length) {
                        output = true;
                        outputPath = args[i + 1];
                    } else {
                        System.out.println("参数错误");
                    }
                    break;
                default:
                    source = args[i];
            }
        }
        if (args.length == 0) {
            System.out.println("  -s        将输入的 c0 源代码翻译为文本汇编文件\n" +
                    "  -c        将输入的 c0 源代码翻译为二进制目标文件\n" +
                    "  -h        显示关于编译器使用的帮助\n" +
                    "  -o file   输出到指定的文件 file");
        } else {
            if (output) {
                syntaxAnalyze(source, outputPath, text);
            } else {
                syntaxAnalyze(source, text);
            }
        }
    }
}
