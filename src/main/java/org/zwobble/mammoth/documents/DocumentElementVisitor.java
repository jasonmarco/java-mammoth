package org.zwobble.mammoth.documents;

public interface DocumentElementVisitor<T> {
    T visit(Paragraph paragraph);
    T visit(Run run);
    T visit(Text text);

    T visit(Tab tab);

    T visit(Table table);
    T visit(TableRow tableRow);
    T visit(TableCell tableCell);
}