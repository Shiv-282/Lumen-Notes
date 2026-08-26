package com.lumen.notes.canvas

/**
 * Reversible document mutation. Commands are pure: they return a new [CanvasDoc],
 * which keeps undo/redo trivially correct.
 */
interface CanvasCommand {
    fun apply(doc: CanvasDoc): CanvasDoc
    fun revert(doc: CanvasDoc): CanvasDoc
}

data class AddStroke(private val stroke: StrokeData) : CanvasCommand {
    override fun apply(doc: CanvasDoc): CanvasDoc = doc.copy(strokes = doc.strokes + stroke)
    override fun revert(doc: CanvasDoc): CanvasDoc = doc.copy(strokes = doc.strokes.filterNot { it.id == stroke.id })
}

data class RemoveStrokes(private val removed: List<StrokeData>) : CanvasCommand {
    private val ids = removed.map { it.id }.toHashSet()

    override fun apply(doc: CanvasDoc): CanvasDoc =
        doc.copy(strokes = doc.strokes.filterNot { it.id in ids })

    override fun revert(doc: CanvasDoc): CanvasDoc =
        doc.copy(strokes = doc.strokes + removed)
}

data class AddText(private val text: TextBlockData) : CanvasCommand {
    override fun apply(doc: CanvasDoc): CanvasDoc = doc.copy(texts = doc.texts + text)
    override fun revert(doc: CanvasDoc): CanvasDoc = doc.copy(texts = doc.texts.filterNot { it.id == text.id })
}

data class RemoveTexts(private val removed: List<TextBlockData>) : CanvasCommand {
    private val ids = removed.map { it.id }.toHashSet()

    override fun apply(doc: CanvasDoc): CanvasDoc =
        doc.copy(texts = doc.texts.filterNot { it.id in ids })

    override fun revert(doc: CanvasDoc): CanvasDoc = doc.copy(texts = doc.texts + removed)
}

/** Content/size/color change of a single text block (upsert semantics). */
data class UpdateText(
    private val id: String,
    private val before: TextBlockData?,
    private val after: TextBlockData
) : CanvasCommand {
    override fun apply(doc: CanvasDoc): CanvasDoc = doc.upsertText(after)

    override fun revert(doc: CanvasDoc): CanvasDoc =
        if (before != null) doc.upsertText(before)
        else doc.copy(texts = doc.texts.filterNot { it.id == id })

    private fun CanvasDoc.upsertText(block: TextBlockData): CanvasDoc =
        if (texts.any { it.id == block.id }) copy(texts = texts.map { if (it.id == block.id) block else it })
        else copy(texts = texts + block)
}

/** Paste: adds a whole fragment. */
data class AddFragment(
    private val strokes: List<StrokeData>,
    private val texts: List<TextBlockData>
) : CanvasCommand {
    private val strokeIds = strokes.map { it.id }.toHashSet()
    private val textIds = texts.map { it.id }.toHashSet()

    override fun apply(doc: CanvasDoc): CanvasDoc =
        doc.copy(strokes = doc.strokes + strokes, texts = doc.texts + texts)

    override fun revert(doc: CanvasDoc): CanvasDoc = doc.copy(
        strokes = doc.strokes.filterNot { it.id in strokeIds },
        texts = doc.texts.filterNot { it.id in textIds }
    )
}

/** Groups several commands into one undo step (applied/reverted in order). */
data class CompositeCommand(private val commands: List<CanvasCommand>) : CanvasCommand {
    constructor(vararg commands: CanvasCommand) : this(commands.toList())

    override fun apply(doc: CanvasDoc): CanvasDoc = commands.fold(doc) { d, c -> c.apply(d) }
    override fun revert(doc: CanvasDoc): CanvasDoc = commands.reversed().fold(doc) { d, c -> c.revert(d) }
}

