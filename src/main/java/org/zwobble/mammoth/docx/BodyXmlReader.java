package org.zwobble.mammoth.docx;

import org.zwobble.mammoth.documents.*;
import org.zwobble.mammoth.results.Result;
import org.zwobble.mammoth.util.MammothOptionals;
import org.zwobble.mammoth.xml.XmlElement;
import org.zwobble.mammoth.xml.XmlElementLike;
import org.zwobble.mammoth.xml.XmlNode;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.zwobble.mammoth.docx.ReadResult.EMPTY_SUCCESS;
import static org.zwobble.mammoth.docx.ReadResult.success;
import static org.zwobble.mammoth.results.Warning.warning;
import static org.zwobble.mammoth.util.MammothLists.list;

public class BodyXmlReader {
    private final Styles styles;
    private final Numbering numbering;

    public BodyXmlReader(Styles styles, Numbering numbering) {
        this.styles = styles;
        this.numbering = numbering;
    }

    public ReadResult readElement(XmlElement element) {
        switch (element.getName()) {
            case "w:t":
                return success(new Text(element.innerText()));
            case "w:r":
                return readRun(element);
            case "w:p":
                return readParagraph(element);

            case "w:tab":
                return success(Tab.TAB);

            case "w:tbl":
                return readElements(element.children()).map(Table::new);
            case "w:tr":
                return readElements(element.children()).map(TableRow::new);
            case "w:tc":
                return readElements(element.children()).map(TableCell::new);

            case "w:pPr":
                return EMPTY_SUCCESS;

            default:
                // TODO: emit warning
                return EMPTY_SUCCESS;
        }
    }

    private ReadResult readRun(XmlElement element) {
        XmlElementLike properties = element.findChildOrEmpty("w:rPr");
        return ReadResult.map(
            readRunStyle(properties),
            readElements(element.children()),
            (style, children) -> new Run(
                isBold(properties),
                isItalic(properties),
                isUnderline(properties),
                isStrikethrough(properties),
                style,
                children));
    }

    private boolean isBold(XmlElementLike properties) {
        return properties.hasChild("w:b");
    }

    private boolean isItalic(XmlElementLike properties) {
        return properties.hasChild("w:i");
    }

    private boolean isUnderline(XmlElementLike properties) {
        return properties.hasChild("w:u");
    }

    private boolean isStrikethrough(XmlElementLike properties) {
        return properties.hasChild("w:strike");
    }

    private Result<Optional<Style>> readRunStyle(XmlElementLike properties) {
        return readStyle(properties, "w:rStyle", "Run", styles::findCharacterStyleById);
    }

    public ReadResult readElements(Iterable<XmlNode> nodes) {
        return ReadResult.concat(
            transform(
                filter(nodes, XmlElement.class),
                this::readElement));
    }

    private ReadResult readParagraph(XmlElement element) {
        XmlElementLike properties = element.findChildOrEmpty("w:pPr");
        Optional<NumberingLevel> numbering = readNumbering(properties);
        return ReadResult.map(
            readParagraphStyle(properties),
            readElements(element.children()),
            (style, children) -> new Paragraph(style, numbering, children));
    }

    private Result<Optional<Style>> readParagraphStyle(XmlElementLike properties) {
        return readStyle(properties, "w:pStyle", "Paragraph", styles::findParagraphStyleById);
    }

    private Result<Optional<Style>> readStyle(
        XmlElementLike properties,
        String styleTagName,
        String styleType,
        Function<String, Optional<Style>> findStyleById)
    {
        return readVal(properties, styleTagName)
            .map(styleId -> findStyleById(styleType, styleId, findStyleById))
            .orElse(Result.empty());
    }

    private Result<Optional<Style>> findStyleById(
        String styleType,
        String styleId,
        Function<String, Optional<Style>> findStyleById)
    {
        Optional<Style> style = findStyleById.apply(styleId);
        if (style.isPresent()) {
            return Result.success(style);
        } else {
            return new Result<>(
                Optional.of(new Style(styleId, Optional.empty())),
                list(warning(styleType + " style with ID " + styleId + " was referenced but not defined in the document")));
        }

    }

    private Optional<NumberingLevel> readNumbering(XmlElementLike properties) {
        XmlElementLike numberingProperties = properties.findChildOrEmpty("w:numPr");
        return MammothOptionals.flatMap(
            readVal(numberingProperties, "w:numId"),
            readVal(numberingProperties, "w:ilvl"),
            numbering::findLevel);
    }

    private Optional<String> readVal(XmlElementLike element, String name) {
        return element.findChildOrEmpty(name).getAttributeOrNone("w:val");
    }
}