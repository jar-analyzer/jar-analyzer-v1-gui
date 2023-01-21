package com.chaitin.jar.analyzer.spel;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Test {
    public static void main(String[] args) {
        MethodEL m = new MethodEL();
        ExpressionParser parser = new SpelExpressionParser();

        String  spel1 = "#method.nameContains('test')" +
                        ".classNameContains('test')"+
                        ".returnContains('int')"+
                        ".paramsNum(3)";

        Expression exp = parser.parseExpression(spel1);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("method",m);
        Object value = exp.getValue(ctx);
        System.out.println(value);
    }
}
