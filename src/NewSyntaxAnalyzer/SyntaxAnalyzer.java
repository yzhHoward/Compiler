package NewSyntaxAnalyzer;

import NewSymbolTable.FunctionSymbols;
import NewSymbolTable.SymbolTable;
import NewSymbolTable.Symbols;
import SymbolTable.SymbolTables;
import WordAnalyzer.WordAnalyzer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

import static NewSymbolTable.SymbolTable.*;
import static NewSyntaxAnalyzer.Utils.tokenToDataType;
import static SymbolTable.SymbolTables.nextLevel;

public class SyntaxAnalyzer {
    public String token;
    public int lineOffset;
    public int wordOffset;
    public Errors error;
    private WordAnalyzer wordAnalyzer;
    private PCodeWriter pCodeWriter;
    private WordAnalyzer.Symbols symbol;
    private ArrayList<NextWord> buf = new ArrayList<>();
    private int cursor = -1;
    private int level = 0;
    private String num;

    public SyntaxAnalyzer(WordAnalyzer wordAnalyzer) {
        this.wordAnalyzer = wordAnalyzer;
    }

    private void read() {
        ++cursor;
        NextWord nextWord = buf.get(cursor);
        token = nextWord.token;
        symbol = nextWord.symbolType;
        lineOffset = nextWord.lineOffset;
        wordOffset = nextWord.wordOffset;
    }

    private void unread() {
        if (cursor > 0) {
            --cursor;
            NextWord nextWord = buf.get(cursor);
            token = nextWord.token;
            symbol = nextWord.symbolType;
            lineOffset = nextWord.lineOffset;
            wordOffset = nextWord.wordOffset;
        } else if (cursor == 0) {
            --cursor;
            token = null;
            symbol = null;
            lineOffset = 1;
            wordOffset = 0;
        } else {
            error = Errors.UnreadError;
        }
    }

    public void start() throws IOException {
        wordAnalyzer.getsym();
        while (wordAnalyzer.symbol != WordAnalyzer.Symbols.EOF) {
            buf.add(new NextWord(wordAnalyzer.token, wordAnalyzer.symbol, wordAnalyzer.lineOffset, wordAnalyzer.wordOffset));
            wordAnalyzer.getsym();
            if (wordAnalyzer.error != null) {
                System.out.println(wordAnalyzer.error + " at " + wordAnalyzer.lineOffset + ":" + (wordAnalyzer.wordOffset - token.length() + 1) + " word: " + wordAnalyzer.token);
                return;
            }
        }
        nextLevel();
        program();
    }

    // 程序
    private void program() {
        read();
        while (symbol == WordAnalyzer.Symbols.Const) {
            unread();
            if (!constantDeclaration()) {
                return;
            }
            read();
        }
        unread();
        while (true) {
            if (!variableDeclaration()) {
                return;
            }
            read();
            read();
            read();
            if (symbol == WordAnalyzer.Symbols.LeftBrace) {
                unread();
                unread();
                unread();
                break;
            } else {
                unread();
                unread();
                unread();
            }
        }
        while (functionDefinition()) {
            if (cursor == buf.size() - 1) {
                if (SymbolTable.findFunctionSymbol("main", 1) == null) {
                    error = Errors.MissingMain;
                }
                return;
            }
        }
    }

    // 常量说明部分
    private boolean constantDeclaration() {
        boolean exist = false;
        SymbolTable.DataType dataType;
        String value;
        read();
        if (symbol == WordAnalyzer.Symbols.Const) {
            read();
        } else {
            unread();
            return true;
        }
        if (symbol == WordAnalyzer.Symbols.Char || symbol == WordAnalyzer.Symbols.Int ||
                symbol == WordAnalyzer.Symbols.Float || symbol == WordAnalyzer.Symbols.Double) {
            dataType = tokenToDataType(token);
        } else {
            error = Errors.ExpectType;
            return false;
        }
        while (symbol == WordAnalyzer.Symbols.Identifier) {
            exist = true;
            if (SymbolTables.findVariableSymbol(token, level)) {
                error = Errors.DuplicateSymbol;
                return false;
            }
            String temp = token;
            read();
            if (symbol != WordAnalyzer.Symbols.Assign) {
                error = Errors.InvalidConstantDeclaration;
                return false;
            }
            read();
            switch (dataType) {
                case SignedChar:
                case UnsignedChar:
                    value = String.valueOf(new BigInteger(token).byteValue());
                case SignedInt:
                case UnsignedInt:
                    value = String.valueOf(new BigInteger(token).intValue());
                case SignedFloat:
                case UnsignedFloat:
                    value = String.valueOf(new BigInteger(token).floatValue());
                case SignedDouble:
                case UnsignedDouble:
                    value = String.valueOf(new BigInteger(token).doubleValue());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + dataType);
            }
            if (!insertVariableSymbol(level, temp, SymbolTable.SymbolType.Variable, dataType, lineOffset, wordOffset, value)) {
                error = Errors.DuplicateSymbol;
                return false;
            }
            read();
            if (symbol == WordAnalyzer.Symbols.Semicolon) {
                return true;
            } else if (symbol == WordAnalyzer.Symbols.Comma) {
                read();
            }
        }
        if (!exist) {
            error = Errors.ExpectIdentifier;
            return false;
        }
        error = Errors.ExpectCorrectSeparator;
        return false;
    }

    // 变量说明部分
    private boolean variableDeclaration() {
        SymbolTable.DataType dataType;
        read();
        if (symbol == WordAnalyzer.Symbols.Char || symbol == WordAnalyzer.Symbols.Int ||
                symbol == WordAnalyzer.Symbols.Float || symbol == WordAnalyzer.Symbols.Double) {
            dataType = tokenToDataType(token);
        } else {
            // 不是声明头部
            unread();
            return true;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.Main) {
            unread();
            unread();
            return true;
        }
        if (symbol != WordAnalyzer.Symbols.Identifier) {
            error = Errors.ExpectIdentifier;
            return false;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.LeftBracket || symbol == WordAnalyzer.Symbols.Comma || symbol == WordAnalyzer.Symbols.Semicolon) {
            unread();
            String temp;
            int length;
            while (symbol == WordAnalyzer.Symbols.Identifier) {
                temp = token;
                read();
                if (symbol == WordAnalyzer.Symbols.LeftBracket) {
                    read();
                    if (symbol != WordAnalyzer.Symbols.UnsignedInt) {
                        error = Errors.ExpectInt32;
                        return false;
                    }
                    try {
                        length = Integer.parseInt(token);
                    } catch (NumberFormatException e) {
                        error = Errors.ExpectInt32;
                        return false;
                    }
                    read();
                    if (symbol != WordAnalyzer.Symbols.RightBracket) {
                        error = Errors.ExpectCorrectSeparator;
                        return false;
                    }
                    if (!insertArraySymbol(level, temp, SymbolTable.SymbolType.Variable, SymbolTable.DataType.Array,
                            length, lineOffset, wordOffset)) {
                        return false;
                    }
                } else {
                    unread();
                    if (!insertVariableSymbol(level, temp, SymbolTable.SymbolType.Variable, dataType, lineOffset, wordOffset)) {
                        error = Errors.DuplicateSymbol;
                        return false;
                    }
                }
                read();
                if (symbol == WordAnalyzer.Symbols.Semicolon) {
                    return true;
                } else if (symbol == WordAnalyzer.Symbols.Assign) {
                    error = Errors.AssignWithDeclaration;
                    return false;
                } else if (symbol != WordAnalyzer.Symbols.Comma) {
                    error = Errors.ExpectCorrectSeparator;
                    return false;
                }
                read();
            }
            error = Errors.ExpectIdentifier;
            return false;
        } else if (symbol == WordAnalyzer.Symbols.LeftParenthesis) {
            // 不是变量声明
            unread();
            unread();
            unread();
            return true;
        } else if (symbol == WordAnalyzer.Symbols.Assign) {
            error = Errors.AssignWithDeclaration;
            return false;
        } else {
            error = Errors.InvalidVariableDeclaration; // UnknownError
            return false;
        }
    }

    // 函数定义部分
    private boolean functionDefinition() {
        SymbolTable.DataType dataType = null;
        String functionName;
        read();
        if (symbol == WordAnalyzer.Symbols.Char || symbol == WordAnalyzer.Symbols.Int || symbol == WordAnalyzer.Symbols.Float ||
                symbol == WordAnalyzer.Symbols.Double || symbol == WordAnalyzer.Symbols.Void) {
            dataType = tokenToDataType(token);
        }
        if (dataType == null) {
            if (symbol == WordAnalyzer.Symbols.Identifier) {
                error = Errors.ExpectReturnType;
            } else {
                error = Errors.InvalidSentenceSequence;
            }
            return false;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.Main) {
            if (dataType != SymbolTable.DataType.SignedInt) {
                error = Errors.InvalidReturnType;
                return false;
            }
//            writeSymbols();
        }
        if (symbol != WordAnalyzer.Symbols.Main && symbol != WordAnalyzer.Symbols.Identifier) {
            error = Errors.ExpectIdentifier;
            return false;
        }
        if (!insertFunctionSymbol(token, SymbolTable.SymbolType.Function, dataType, lineOffset, wordOffset)) {
            error = Errors.DuplicateSymbol;
            return false;
        }
        ++level;
        nextLevel();
        functionName = token;
        if (!argument(functionName)) {
            return false;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.LeftBrace) {
            read();
            while (symbol == WordAnalyzer.Symbols.Const) {
                unread();
                if (!constantDeclaration()) {
                    return false;
                }
                read();
            }
            unread();
            while (true) {
                read();
                if (symbol == WordAnalyzer.Symbols.Char || symbol == WordAnalyzer.Symbols.Int ||
                        symbol == WordAnalyzer.Symbols.Float || symbol == WordAnalyzer.Symbols.Double) {
                    unread();
                } else {
                    break;
                }
                if (!variableDeclaration()) {
                    return false;
                }
            }
            if (!statementSequence()) {
                return false;
            }
        } else {
            error = Errors.MissingFunctionBody;
            return false;
        }
        if (dataType == DataType.Void) {
            pCodeWriter.write("RET", 0, 0);
            return true;
        }
        pCodeWriter.write("LIT", 0, 0);
        pCodeWriter.write("STO", 0, 0);
        pCodeWriter.write("RET", 0, 0);
        return true;
    }

    // 参数
    private boolean argument(String functionName) {
        DataType dataType;
        read();
        if (symbol == WordAnalyzer.Symbols.LeftParenthesis) {
            read();
            if (symbol == WordAnalyzer.Symbols.RightParenthesis) {
                return true;
            }
            unread();
            while (true) {
                read();
                if (symbol == WordAnalyzer.Symbols.Char || symbol == WordAnalyzer.Symbols.Int ||
                        symbol == WordAnalyzer.Symbols.Float || symbol == WordAnalyzer.Symbols.Double) {
                    dataType = tokenToDataType(token);
                } else {
                    error = Errors.ExpectType;
                    return false;
                }
                read();
                if (symbol != WordAnalyzer.Symbols.Identifier) {
                    error = Errors.ExpectIdentifier;
                    return false;
                }
                if (!updateFunctionSymbol(functionName, token, dataType, lineOffset, wordOffset)) {
                    error = Errors.DuplicateSymbol;
                    return false;
                }
                read();
                if (symbol == WordAnalyzer.Symbols.RightParenthesis) {
                    return true;
                } else if (symbol != WordAnalyzer.Symbols.Comma) {
                    error = Errors.ExpectCorrectSeparator;
                    return false;
                }
            }
        } else {
            error = Errors.InvalidArgumentDeclaration; // Unknown
            return false;
        }
    }

    // 语句序列
    private boolean statementSequence() {
        read();
        if (symbol == WordAnalyzer.Symbols.RightBrace) {
            --level;
            prevLevel();
            return true;
        }
        unread();
        while (statement()) {
            if (error != null) {
                return false;
            }
            read();
            if (symbol == WordAnalyzer.Symbols.RightBrace) {
                --level;
                prevLevel();
                return true;
            }
            unread();
        }
        return false;
    }

    // 语句 在这里true表示已经成功处理，false表示没有语句或者遇到异常了
    private boolean statement() {
        Symbols symbols;
        read();
        switch (symbol) {
            case If:
                return ifStatement();
            case While:
                return whileStatement();
            case For:
//                return forStatement();
            case LeftBrace:
                ++level;
                nextLevel();
                return statementSequence();
            case Identifier:
                if ((symbols = findVariableSymbol(token)) == null) {
                    error = Errors.SymbolNotFound;
                    return false;
                }
                if (symbols.symbolType == SymbolType.Function) {
                    unread();
                    if (!callFunction()) {
                        return false;
                    }
                    read();
                    if (symbol != WordAnalyzer.Symbols.Semicolon) {
                        error = Errors.ExpectCorrectSeparator;
                        return false;
                    }
                    return true;
                } else {
                    unread();
//                    if (!assignStatement()) {
//                        return false;
//                    }
                    read();
                    if (symbol != WordAnalyzer.Symbols.Semicolon) {
                        error = Errors.ExpectCorrectSeparator;
                        return false;
                    }
                    return true;
                }
            case Scanf:
//                if (!scanf()) {
//                    return false;
//                }
                read();
                if (symbol != WordAnalyzer.Symbols.Semicolon) {
                    error = Errors.ExpectCorrectSeparator;
                    return false;
                }
                return true;
            case Printf:
//                if (!printf()) {
//                    return false;
//                }
                read();
                if (symbol != WordAnalyzer.Symbols.Semicolon) {
                    error = Errors.ExpectCorrectSeparator;
                    return false;
                }
                return true;
            case Return:
//                if (!returnAnalyse()) {
//                    return false;
//                }
                read();
                if (symbol != WordAnalyzer.Symbols.Semicolon) {
                    error = Errors.ExpectCorrectSeparator;
                    return false;
                }
            case Semicolon:
                return true;
            default:
                error = Errors.InvalidArgumentDeclaration;
                return false;
                /*System.out.println("这句话不应该出现的");
                return false;*/
        }
    }

    // 条件语句
    private boolean ifStatement() {
        read();
        if (symbol != WordAnalyzer.Symbols.LeftParenthesis) {
            error = Errors.ExpectCorrectSeparator;
            return false;
        }
//        if (!condition()) {
//            return false;
//        }
        read();
        if (symbol != WordAnalyzer.Symbols.RightParenthesis) {
            error = Errors.ExpectCorrectSeparator;
            return false;
        }
        if (!statement()) {
            return false;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.Else) {
            return statement();
        }
        unread();
        return true;
    }

    // 循环语句
    private boolean whileStatement() {
        read();
        if (symbol != WordAnalyzer.Symbols.LeftParenthesis) {
            error = Errors.ExpectCorrectSeparator;
            return false;
        }
//        if (!condition()) {
//            return false;
//        }
        read();
        if (symbol != WordAnalyzer.Symbols.RightParenthesis) {
            error = Errors.ExpectCorrectSeparator;
            return false;
        }
        return statement();
    }

    // 函数调用语句
    private boolean callFunction() {
        FunctionSymbols functionSymbol;
        read();
        if (symbol != WordAnalyzer.Symbols.Identifier) {
            error = Errors.ExpectIdentifier;
            return false;
        }
        if ((functionSymbol = findFunctionSymbol(token)) == null) {
            error = Errors.SymbolNotFound;
            return false;
        }
        read();
        if (symbol != WordAnalyzer.Symbols.LeftParenthesis) {
            error = Errors.ExpectCorrectSeparator;
            return false;
        }
        read();
        if (symbol == WordAnalyzer.Symbols.RightParenthesis) {
            return true;
        } else {
            unread();
//            while (expression()) {
//                read();
//                if (symbol == WordAnalyzer.Symbols.RightParenthesis) {
//                    return true;
//                } else if (symbol != WordAnalyzer.Symbols.Comma) {
//                    error = Errors.ExpectCorrectSeparator;
//                    return false;
//                }
//            }
            return false;
        }
    }

    public enum Errors {
        InvalidConstantDeclaration,
        InvalidVariableDeclaration,
        DuplicateSymbol,
        SymbolNotFound,
        ExpectIdentifier,
        ExpectInt32,
        ExpectCorrectSeparator,
        ExpectType,
        ExpectReturnType,
        InvalidFunctionDeclaration,
        InvalidArgumentDeclaration,
        MissingFunctionBody,
        InvalidSentenceSequence,
        InvalidExpression,
        InvalidFactor,
        InvalidIfStatement,
        InvalidWhileStatement,
        InvalidAssignment,
        InvalidReturn,
        InvalidReturnType,
        InvalidScanf,
        InvalidPrintf,
        InvalidCall,
        InvalidCondition,
        LessArguments,
        MoreArguments,
        AssignToConstant,
        AssignToFunction,
        AssignWithDeclaration,
        MissingMain,
        MissingSemicolon,
        MissingSentence,
        ReturnValueForVoidFunction,
        NoReturnValueForIntFunction,
        NotCallingFunction,
        UnsupportedFeature,
        UnreadError
    }
}
