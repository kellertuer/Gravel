// $ANTLR : "Stringparser.g" -> "StringParser.java"$

	package etc;
import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import antlr.ASTFactory;
import antlr.ASTPair;

public class StringParser extends antlr.LLkParser       implements StringLexerTokenTypes
 {

protected StringParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public StringParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected StringParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public StringParser(TokenStream lexer) {
  this(lexer,1);
}

public StringParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final double  expr() throws RecognitionException, TokenStreamException {
		double r=0;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expr_AST = null;
		double x;
		
		try {      // for error handling
			r=mexpr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop20:
			do {
				switch ( LA(1)) {
				case PLUS:
				{
					AST tmp1_AST = null;
					tmp1_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp1_AST);
					match(PLUS);
					x=mexpr();
					astFactory.addASTChild(currentAST, returnAST);
					r+=x;
					break;
				}
				case MINUS:
				{
					AST tmp2_AST = null;
					tmp2_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp2_AST);
					match(MINUS);
					x=mexpr();
					astFactory.addASTChild(currentAST, returnAST);
					r-=x;
					break;
				}
				default:
				{
					break _loop20;
				}
				}
			} while (true);
			}
			expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		returnAST = expr_AST;
		return r;
	}
	
	public final double  mexpr() throws RecognitionException, TokenStreamException {
		double r=0;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST mexpr_AST = null;
		double x;
		
		try {      // for error handling
			r=dexpr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop23:
			do {
				switch ( LA(1)) {
				case MUL:
				{
					AST tmp3_AST = null;
					tmp3_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp3_AST);
					match(MUL);
					x=dexpr();
					astFactory.addASTChild(currentAST, returnAST);
					r *=x;
					break;
				}
				case DIV:
				{
					AST tmp4_AST = null;
					tmp4_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp4_AST);
					match(DIV);
					x=dexpr();
					astFactory.addASTChild(currentAST, returnAST);
					r /=x;
					break;
				}
				default:
				{
					break _loop23;
				}
				}
			} while (true);
			}
			mexpr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		returnAST = mexpr_AST;
		return r;
	}
	
	public final double  dexpr() throws RecognitionException, TokenStreamException {
		double r=0;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST dexpr_AST = null;
		double x;
		
		try {      // for error handling
			r=atom();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop26:
			do {
				if ((LA(1)==EXP)) {
					AST tmp5_AST = null;
					tmp5_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp5_AST);
					match(EXP);
					x=atom();
					astFactory.addASTChild(currentAST, returnAST);
					r = Math.pow(r,x);
				}
				else {
					break _loop26;
				}
				
			} while (true);
			}
			dexpr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		returnAST = dexpr_AST;
		return r;
	}
	
	public final double  atom() throws RecognitionException, TokenStreamException {
		double r=0;
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST atom_AST = null;
		Token  i = null;
		AST i_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case NUM:
			{
				i = LT(1);
				i_AST = astFactory.create(i);
				astFactory.addASTChild(currentAST, i_AST);
				match(NUM);
				r=(double)Double.parseDouble(i.getText());
				atom_AST = (AST)currentAST.root;
				break;
			}
			case LKLAMMER:
			{
				match(LKLAMMER);
				r=expr();
				astFactory.addASTChild(currentAST, returnAST);
				match(RKLAMMER);
				atom_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
		returnAST = atom_AST;
		return r;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"WS",
		"LKLAMMER",
		"RKLAMMER",
		"MUL",
		"DIV",
		"EXP",
		"PLUS",
		"MINUS",
		"SEMI",
		"ZIFFER",
		"NUM"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 64L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 3136L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 3520L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 4032L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	
	}
