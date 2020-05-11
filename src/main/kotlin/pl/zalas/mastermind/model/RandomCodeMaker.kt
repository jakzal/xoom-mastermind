package pl.zalas.mastermind.model

import pl.zalas.mastermind.model.Code.CodePeg

class RandomCodeMaker(private val codeLength: Int) : CodeMaker {
    override fun invoke() = Code(
        (1..codeLength).map { CodePeg.values().random() }
    )
}
