package pl.zalas.mastermind.test

import pl.zalas.mastermind.model.Code
import pl.zalas.mastermind.model.CodeMaker

class FakeCodeMaker(private val code: Code) : CodeMaker {
    override fun invoke(): Code = code
}
