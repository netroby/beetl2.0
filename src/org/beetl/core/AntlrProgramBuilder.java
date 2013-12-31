package org.beetl.core;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.beetl.core.attr.ObjectAA;
import org.beetl.core.parser.BeetlParser;
import org.beetl.core.parser.BeetlParser.AssignGeneralContext;
import org.beetl.core.parser.BeetlParser.AssignMentContext;
import org.beetl.core.parser.BeetlParser.BlockStContext;
import org.beetl.core.parser.BeetlParser.BooleanLiteralContext;
import org.beetl.core.parser.BeetlParser.BreakStContext;
import org.beetl.core.parser.BeetlParser.ConstantsTextStatmentContext;
import org.beetl.core.parser.BeetlParser.ContinueStContext;
import org.beetl.core.parser.BeetlParser.ExpressionContext;
import org.beetl.core.parser.BeetlParser.ForControlContext;
import org.beetl.core.parser.BeetlParser.ForStContext;
import org.beetl.core.parser.BeetlParser.LiteralExpContext;
import org.beetl.core.parser.BeetlParser.ReturnStContext;
import org.beetl.core.parser.BeetlParser.StatementContext;
import org.beetl.core.parser.BeetlParser.StaticOutputStContext;
import org.beetl.core.parser.BeetlParser.TextOutputStContext;
import org.beetl.core.parser.BeetlParser.TextStatmentContext;
import org.beetl.core.parser.BeetlParser.TextVarContext;
import org.beetl.core.parser.BeetlParser.TextformatContext;
import org.beetl.core.parser.BeetlParser.VarAttributeArrayOrMapContext;
import org.beetl.core.parser.BeetlParser.VarAttributeContext;
import org.beetl.core.parser.BeetlParser.VarAttributeGeneralContext;
import org.beetl.core.parser.BeetlParser.VarAttributeVirtualContext;
import org.beetl.core.parser.BeetlParser.VarRefContext;
import org.beetl.core.parser.BeetlParser.VarRefExpContext;
import org.beetl.core.parser.BeetlParser.VarStContext;
import org.beetl.core.statement.ASTNode;
import org.beetl.core.statement.BlockStatement;
import org.beetl.core.statement.BreakStatement;
import org.beetl.core.statement.ContinueStatement;
import org.beetl.core.statement.Expression;
import org.beetl.core.statement.ForStatement;
import org.beetl.core.statement.IGoto;
import org.beetl.core.statement.Literal;
import org.beetl.core.statement.PlaceholderST;
import org.beetl.core.statement.ProgramMetaData;
import org.beetl.core.statement.ReturnStatement;
import org.beetl.core.statement.SafePlaceholderST;
import org.beetl.core.statement.Statement;
import org.beetl.core.statement.StaticTextASTNode;
import org.beetl.core.statement.VarAssignStatement;
import org.beetl.core.statement.VarAssignStatementSeq;
import org.beetl.core.statement.VarAttribute;
import org.beetl.core.statement.VarDefineNode;
import org.beetl.core.statement.VarRef;
import org.beetl.core.statement.VarSquareAttribute;
import org.beetl.core.statement.VarVirtualAttribute;

public class AntlrProgramBuilder {

	ProgramMetaData data = new ProgramMetaData();
	ProgramBuilderContext pbCtx = new ProgramBuilderContext();

	public AntlrProgramBuilder() {

	}

	public ProgramMetaData build(ParseTree tree) {

		int size = tree.getChildCount() - 1;
		List<Statement> ls = new ArrayList<Statement>(size);
		for (int i = 0; i < size; i++) {
			ls.add(parseStatment((ParserRuleContext) tree.getChild(i)));

		}

		pbCtx.anzlyszeGlobal();
		pbCtx.anzlyszeLocal();
		data.varIndexSize = pbCtx.varIndexSize;
		data.tempVarStartIndex = pbCtx.globalIndexMap.size();
		data.statements = ls.toArray(new Statement[0]);
		data.globalIndexMap = pbCtx.globalIndexMap;
		data.globalVarAttr = pbCtx.globaVarAttr;

		return data;

	}

	private Statement parseStatment(ParserRuleContext node) {

		if (node instanceof VarStContext) {
			return parseVarSt((VarStContext) node);

		} else if (node instanceof BlockStContext) {
			BlockStContext bc = (BlockStContext) node;
			Statement block = parseBlock(bc.block().statement());
			return block;
		} else if (node instanceof TextOutputStContext) {
			return this.parseTextOutputSt((TextOutputStContext) node);
		} else if (node instanceof ReturnStContext) {
			ReturnStatement st = new ReturnStatement(null);
			return st;
		} else if (node instanceof BreakStContext) {
			BreakStatement st = new BreakStatement(null);
			return st;
		} else if (node instanceof ContinueStContext) {
			ContinueStatement st = new ContinueStatement(null);
			return st;
		} else if (node instanceof ForStContext) {
			ForStatement forStatement = parseForSt((ForStContext) node);
			return forStatement;
		} else if (node instanceof StaticOutputStContext) {
			StaticOutputStContext st = (StaticOutputStContext) node;
			ConstantsTextStatmentContext cst = st.constantsTextStatment();
			String str = cst.DecimalLiteral().getSymbol().getText();
			int position = Integer.parseInt(str);
			StaticTextASTNode textNode = new StaticTextASTNode(position, null);
			return textNode;
		} else {
			throw new UnsupportedOperationException();
		}

	}

	protected ForStatement parseForSt(ForStContext ctx) {
		pbCtx.enterBlock();
		ForControlContext forCtx = ctx.forControl();
		VarDefineNode forVar = new VarDefineNode(this.getBTToken(forCtx
				.Identifier().getSymbol()));

		VarDefineNode loopStatusVar = new VarDefineNode(
				new org.beetl.core.statement.Token(forCtx.Identifier()
						.getSymbol().getText()
						+ "LP", forCtx.Identifier().getSymbol().getLine(), 0));

		pbCtx.addVarAndPostion(forVar);
		pbCtx.addVarAndPostion(loopStatusVar);

		StatementContext forContext = ctx.statement(0);
		StatementContext elseContext = null;
		Statement forPart = this.parseStatment(forContext);
		Statement elseForPart = null;
		if (ctx.Elsefor() != null) {
			elseContext = ctx.statement(1);
			elseForPart = this.parseStatment(elseContext);

		}

		Expression exp = this.parseExpress(forCtx.expression());
		ForStatement forStatement = new ForStatement(forVar, exp, forPart,
				elseForPart, forVar.token);

		pbCtx.exitBlock();
		return forStatement;

	}

	protected Statement parseTextOutputSt(TextOutputStContext ctx) {

		TextStatmentContext tsc = ctx.textStatment();
		boolean isSafe = false;
		if (tsc.NOT() != null) {
			isSafe = true;
		}
		TextVarContext tvc = tsc.textVar();
		if (tvc.COMMA() != null) {
			TextformatContext tfc = tvc.textformat();
			// todo ignore
		}

		Expression exp = this.parseExpress(tvc.expression());
		if (isSafe) {
			SafePlaceholderST placeholder = new SafePlaceholderST(exp, null);
			return placeholder;
		} else {
			PlaceholderST placeholder = new PlaceholderST(exp, null);
			return placeholder;
		}
	}

	private VarAssignStatementSeq parseVarSt(VarStContext node) {
		VarStContext varSt = (VarStContext) node;
		List<AssignMentContext> list = varSt.varDeclareList().assignMent();
		List<ASTNode> listNode = new ArrayList<ASTNode>();
		for (AssignMentContext amc : list) {
			if (amc instanceof AssignGeneralContext) {
				AssignGeneralContext agc = (AssignGeneralContext) amc;
				ExpressionContext expCtx = agc.expression();
				Expression exp = parseExpress(expCtx);
				VarAssignStatement vas = new VarAssignStatement(exp,
						getBTToken(agc.Identifier().getSymbol()));
				listNode.add(vas);
				pbCtx.addVar(vas.token.text);
				pbCtx.setVarPosition(vas.token.text, vas);
			}
			// 其他还有Identifier,Identifier ASSIN block
		}
		VarAssignStatementSeq seq = new VarAssignStatementSeq(
				listNode.toArray(new Statement[0]), null);
		return seq;
	}

	protected Expression parseExpress(ExpressionContext ctx) {
		if (ctx instanceof LiteralExpContext) {
			return parseLiteralExpress((LiteralExpContext) ctx);
		} else if (ctx instanceof VarRefExpContext) {
			return this.parseVarRefExpression((VarRefExpContext) ctx);
		} else {
			return null;
		}
	}

	protected Expression parseVarRefExpression(VarRefExpContext ctx) {
		VarRefContext varRef = ctx.varRef();

		Expression safeExp = null;
		if (ctx.NOT() != null) {
			ExpressionContext safeExpression = ctx.expression();
			safeExp = this.parseExpress(safeExpression);

		}
		List<VarAttributeContext> list = varRef.varAttribute();
		List<VarAttribute> listVarAttr = new ArrayList<VarAttribute>();
		boolean isFirstAttr = true;
		for (VarAttributeContext vac : list) {
			if (vac instanceof VarAttributeGeneralContext) {
				VarAttributeGeneralContext zf = (VarAttributeGeneralContext) vac;
				VarAttribute attr = new VarAttribute(this.getBTToken(zf
						.Identifier().getSymbol()));
				listVarAttr.add(attr);
				if (isFirstAttr) {
					pbCtx.setVarAttr(varRef.Identifier().getText(), zf
							.Identifier().getText());
					isFirstAttr = false;
				}
				attr.setAA(ObjectAA.defaultObjectAA());

			} else if (vac instanceof VarAttributeArrayOrMapContext) {
				VarAttributeArrayOrMapContext zf = (VarAttributeArrayOrMapContext) vac;
				Expression exp = this.parseExpress(zf.expression());
				VarSquareAttribute attr = new VarSquareAttribute(exp, null);
				listVarAttr.add(attr);
			} else if (vac instanceof VarAttributeVirtualContext) {
				VarAttributeVirtualContext zf = (VarAttributeVirtualContext) vac;
				VarVirtualAttribute attr = new VarVirtualAttribute(
						this.getBTToken(zf.Identifier().getSymbol()));
			}
		}

		VarRef var = new VarRef(listVarAttr.toArray(new VarAttribute[0]),
				safeExp, this.getBTToken(varRef.Identifier().getSymbol()));
		pbCtx.setVarPosition(varRef.Identifier().getText(), var);
		return var;
	}

	protected Expression parseLiteralExpress(LiteralExpContext ctx) {
		LiteralExpContext lec = (LiteralExpContext) ctx;
		ParseTree tree = lec.literal().getChild(0);
		Object value = null;
		if (tree instanceof TerminalNode) {
			Token node = ((TerminalNode) tree).getSymbol();
			String strValue = node.getText();

			int type = node.getType();
			switch (type) {
			case BeetlParser.StringLiteral:
				value = strValue;
				break;
			case BeetlParser.FloatingPointLiteral:
				value = Double.parseDouble(strValue);
				break;
			case BeetlParser.DecimalLiteral:
				value = Integer.parseInt(strValue);
				break;
			case BeetlParser.NULL:
				value = null;
				break;

			}

		} else {
			BooleanLiteralContext blc = (BooleanLiteralContext) tree;
			String strValue = blc.getChild(0).getText();
			value = Boolean.parseBoolean(strValue);
		}

		Literal literal = new Literal(value, null);
		return literal;

	}

	protected BlockStatement parseBlock(List list) {
		pbCtx.enterBlock();
		ASTNode[] statements = new ASTNode[list.size()];
		List<Statement> nodes = new ArrayList<Statement>();
		for (int i = 0; i < statements.length; i++) {
			nodes.add(parseStatment((ParserRuleContext) list.get(i)));

		}

		BlockStatement block = new BlockStatement(
				nodes.toArray(new Statement[0]), null);
		switch (pbCtx.current.gotoValue) {
		case IGoto.NORMAL:
			break;
		case IGoto.CONTINUE:
		case IGoto.BREAK:
			block.setGoto(true);
			break;
		case IGoto.RETURN:
			block.setGoto(true);
			if (pbCtx.current.parent != null) {
				pbCtx.current.parent.gotoValue = IGoto.RETURN;
			}

		}
		pbCtx.exitBlock();
		return block;
	}

	public org.beetl.core.statement.Token getBTToken(Token t) {
		org.beetl.core.statement.Token token = new org.beetl.core.statement.Token(
				t.getText(), t.getLine(), t.getCharPositionInLine());
		return token;
	}

}
