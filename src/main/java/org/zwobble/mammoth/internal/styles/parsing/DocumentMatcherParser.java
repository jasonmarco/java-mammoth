package org.zwobble.mammoth.internal.styles.parsing;

import org.zwobble.mammoth.internal.documents.NumberingLevel;
import org.zwobble.mammoth.internal.styles.HtmlPath;
import org.zwobble.mammoth.internal.styles.ParagraphMatcher;
import org.zwobble.mammoth.internal.styles.RunMatcher;
import org.zwobble.mammoth.internal.styles.StyleMapBuilder;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.BiConsumer;

public class DocumentMatcherParser {
    public static BiConsumer<StyleMapBuilder, HtmlPath> parse(TokenIterator<TokenType> tokens) {
        Token<TokenType> identifier = tokens.next(TokenType.IDENTIFIER);
        switch (identifier.getValue()) {
            case "p":
                ParagraphMatcher paragraph = parseParagraphMatcher(tokens);
                return (builder, path) -> builder.mapParagraph(paragraph, path);
            case "r":
                RunMatcher run = parseRunMatcher(tokens);
                return (builder, path) -> builder.mapRun(run, path);
            case "b":
                return StyleMapBuilder::bold;
            case "i":
                return StyleMapBuilder::italic;
            case "u":
                return StyleMapBuilder::underline;
            case "strike":
                return StyleMapBuilder::strikethrough;
            case "comment-reference":
                return StyleMapBuilder::commentReference;
            default:
                throw new LineParseException(identifier, "Unrecognised document element: " + identifier);
        }
    }

    public static ParagraphMatcher parseParagraphMatcher(TokenIterator<TokenType> tokens) {
        Optional<String> styleId = parseStyleId(tokens);
        Optional<String> styleName = parseStyleName(tokens);
        Optional<NumberingLevel> numbering = parseNumbering(tokens);
        return new ParagraphMatcher(styleId, styleName, numbering);
    }

    public static RunMatcher parseRunMatcher(TokenIterator<TokenType> tokens) {
        Optional<String> styleId = parseStyleId(tokens);
        Optional<String> styleName = parseStyleName(tokens);
        return new RunMatcher(styleId, styleName);
    }

    private static Optional<String> parseStyleId(TokenIterator<TokenType> tokens) {
        if (tokens.peekTokenType() == TokenType.CLASS_NAME) {
            return Optional.of(TokenParser.parseClassName(tokens));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> parseStyleName(TokenIterator<TokenType> tokens) {
        if (tokens.peekTokenType() == TokenType.OPEN_SQUARE_BRACKET) {
            tokens.skip();
            tokens.skip(TokenType.IDENTIFIER, "style-name");
            tokens.skip(TokenType.EQUALS);
            String value = tokens.nextValue(TokenType.STRING);
            tokens.skip(TokenType.CLOSE_SQUARE_BRACKET);
            return Optional.of(value.substring(1, value.length() - 1));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<NumberingLevel> parseNumbering(TokenIterator<TokenType> tokens) {
        if (tokens.peekTokenType() == TokenType.COLON) {
            tokens.skip();
            boolean isOrdered = parseListType(tokens);
            tokens.skip(TokenType.OPEN_PAREN);
            String level = new BigInteger(tokens.nextValue(TokenType.INTEGER)).subtract(BigInteger.ONE).toString();
            tokens.skip(TokenType.CLOSE_PAREN);
            return Optional.of(new NumberingLevel(level, isOrdered));
        } else {
            return Optional.empty();
        }
    }

    private static boolean parseListType(TokenIterator<TokenType> tokens) {
        Token<TokenType> listType = tokens.next(TokenType.IDENTIFIER);
        switch (listType.getValue()) {
            case "ordered-list":
                return true;
            case "unordered-list":
                return false;
            default:
                throw new LineParseException(listType, "Unrecognised list type: " + listType);
        }
    }
}
