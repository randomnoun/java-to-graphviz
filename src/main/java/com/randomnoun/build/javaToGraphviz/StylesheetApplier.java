package com.randomnoun.build.javaToGraphviz;

// copied from https://github.com/vilterp/StylesheetApplier/blob/master/src/StylesheetApplier.java
// by knoxg on 2021-10-22 ( no license provided )


import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.parser.CSSOMParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.NegativeCondition;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SimpleSelector;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.CSSValue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StylesheetApplier {

	private Document htmlDocument;
	private CSSStyleSheet styleSheet;
	private static final byte SHIFT_A = 24;
	private static final byte SHIFT_B = 16;
	private static final byte SHIFT_C = 8;

	public static final String INDENT = "\t";

	private static final Ordering<StyleApplication> orderBySpecificity = Ordering.natural().onResultOf(
			new Function<StyleApplication, Integer>() {
				@Override
				public Integer apply( StyleApplication rule ) {
					return rule.getSpecificity();
				}
			});

	static class ExceptionErrorHandler implements ErrorHandler {

		@Override
		public void warning( CSSParseException e ) throws CSSException {
			throw e;
		}

		@Override
		public void error( CSSParseException e ) throws CSSException {
			throw e;
		}

		@Override
		public void fatalError( CSSParseException e ) throws CSSException {
			throw e;
		}

	}

	public StylesheetApplier( Document htmlDocument, CSSStyleSheet styleSheet ) {
		this.htmlDocument = htmlDocument;
		this.styleSheet = styleSheet;
		this.htmlDocument.outputSettings().indentAmount( 4 );
	}

	public Document getHtmlDocument() {
		return htmlDocument;
	}

	public static StylesheetApplier initFromSource( String html, String css ) throws CSSException {
		// parse css
		CSSOMParser parser = new CSSOMParser();
		parser.setErrorHandler( new ExceptionErrorHandler() );
		InputSource cssInputSource = new InputSource( new StringReader( css ) );
		CSSStyleSheet theStyleSheet = null;
		try {
			theStyleSheet = parser.parseStyleSheet( cssInputSource, null, null );
		} catch ( IOException e ) {
			// this should never happen, because we're just reading from a StringReader
			e.printStackTrace();
		}

		// parse HTML
		Document theDocument = Jsoup.parse( html );

		return new StylesheetApplier( theDocument, theStyleSheet );
	}

	public static String applyStylesheet( String html, String css ) {
		StylesheetApplier applier = initFromSource( html, css );
		applier.apply();
		return applier.prettyPrintDocument();
	}

	/**
	 * Initialize a {@code StylesheetApplier} from an HTML file, reading references
	 * to CSS files in {@literal <link rel="stylesheet" type="text/css" href="..."/>} tags.
	 *
	 * @param htmlFile
	 * @return a new {@code StylesheetApplier} instance
	 */
	public static StylesheetApplier initFromFile( File htmlFile ) throws IOException, CSSException {
		// parse document
		Document document = Jsoup.parse( htmlFile, "utf-8" );
		// gather referenced stylesheets
		Elements linkTags = document.head().getElementsByTag( "link" );
		List<File> stylesheetRefs = new ArrayList<File>();
		for ( Element linkTag : linkTags ) {
			boolean isStylesheetLink = linkTag.attr( "rel" ).equals( "stylesheet" ) &&
					linkTag.attr( "type" ).equals( "text/css" );
			String href = null;
			if ( isStylesheetLink ) {
				href = linkTag.attr( "href" );
				linkTag.remove();
				stylesheetRefs.add( new File( href ) );
			}
		}
		// parse 'em
		CSSOMParser cssParser = new CSSOMParser();
		cssParser.setErrorHandler( new ExceptionErrorHandler() );
		List<CSSStyleSheet> styleSheets = new ArrayList<CSSStyleSheet>();
		for ( File stylesheetRef : stylesheetRefs ) {
			InputSource inputSource = new InputSource( new FileReader( stylesheetRef ) );
			CSSStyleSheet styleSheet = cssParser.parseStyleSheet( inputSource, null, stylesheetRef.getAbsolutePath() );
			styleSheets.add( styleSheet );
		}
		// merge 'em into one stylesheet
		CSSStyleSheet combined = new CSSStyleSheetImpl();
		for ( CSSStyleSheet styleSheet : styleSheets ) {
			CSSRuleList rules = styleSheet.getCssRules();
			for ( int i = 0; i < rules.getLength(); i++ ) {
				combined.insertRule( rules.item( i ).getCssText(), combined.getCssRules().getLength() );
			}
		}
		// return new applier
		return new StylesheetApplier( document, combined );
	}

	/**
	 * Given a File which is a path to an HTML file that has <link /> tags in it pointing to
	 * CSS files in the local filesystem, inline the styles in that stylesheet, returning
	 * the HTML result.
	 *
	 * @param htmlFile the file, with link tags in it
	 * @return HTML with styles inlined
	 * @throws IOException
	 * @throws CSSException
	 */
	public static String processFile( File htmlFile ) throws IOException, CSSException {
		StylesheetApplier applier = initFromFile( htmlFile );
		applier.apply();
		return applier.prettyPrintDocument();
	}

	/**
	 * actually inline the styles
	 */
	public void apply() {

		final Multimap<Element, StyleApplication> elementMatches = ArrayListMultimap.create();

		final CSSOMParser inlineParser = new CSSOMParser();
		inlineParser.setErrorHandler( new ExceptionErrorHandler() );

		// factor in elements' style attributes
		NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof Element && node.hasAttr( "style" ) ) {
                    // parse the CSS into a CSSStyleDeclaration
                    InputSource input = new InputSource( new StringReader( node.attr( "style" ) ) );
                    CSSStyleDeclaration declaration = null;
                    try {
                        declaration = inlineParser.parseStyleDeclaration( input );
                    } catch ( IOException e ) {
                        // again, this should never happen, cuz we're just reading from a string
                        e.printStackTrace();
                    }
                    node.removeAttr( "style" );
                    elementMatches.put( ((Element) node), new InlineStyleApplication( declaration ) );
                }
            }

            @Override
            public void tail( Node node, int depth ) {}
        }, htmlDocument.body() );

		// compute which rules match which elements
		CSSRuleList rules = styleSheet.getCssRules();
		for ( int i = 0; i < rules.getLength(); i++ ) {
			if ( rules.item( i ) instanceof CSSStyleRule) {
				CSSStyleRuleImpl rule = (CSSStyleRuleImpl) rules.item( i );
				// for each selector in the rule... (separated by commas)
				for ( int j = 0; j < rule.getSelectors().getLength(); j++ ) {
					Selector selector = rule.getSelectors().item( j );
					Elements matches = null;
					matches = htmlDocument.select( selector.toString() );
					// for each matched element....
					for ( Element match: matches ) {
						elementMatches.put( match, new RuleStyleApplication( selector, rule.getStyle() ) );
					}
				}
			}
		}

		Map<Element, CSSStyleDeclaration> inlinedStyles = new HashMap<Element, CSSStyleDeclaration>();

		// calculate overrides
		for ( Element element : elementMatches.keySet() ) {
			CSSStyleDeclaration properties = new CSSStyleDeclarationImpl();
			List<StyleApplication> matchedRules = new ArrayList<StyleApplication>( elementMatches.get( element ) );
			Collections.sort( matchedRules, orderBySpecificity );
			for ( StyleApplication matchedRule : matchedRules ) {
				CSSStyleDeclaration cssBlock = matchedRule.getCssBlock();
				for ( int i = 0; i < cssBlock.getLength(); i++ ) {
					String propertyKey = cssBlock.item( i );
					CSSValue propertyValue = cssBlock.getPropertyCSSValue( propertyKey );
					// TODO: !important
					properties.setProperty( propertyKey, propertyValue.getCssText(), "" );
				}
			}
			inlinedStyles.put(element, properties);
		}

		// apply to DOM
		for ( Map.Entry<Element, CSSStyleDeclaration> entry : inlinedStyles.entrySet() ) {
			entry.getKey().attr( "style", entry.getValue().getCssText() );
		}

	}

	private abstract class StyleApplication {

		private CSSStyleDeclaration cssBlock;

		public StyleApplication( CSSStyleDeclaration cssBlock ) {
			this.cssBlock = cssBlock;
		}

		public CSSStyleDeclaration getCssBlock() {
			return cssBlock;
		}

		public abstract int getSpecificity();

	}

	private class InlineStyleApplication extends StyleApplication {

		public InlineStyleApplication( CSSStyleDeclaration cssBlock ) {
			super(cssBlock);
		}

		public int getSpecificity() {
			return 1 << SHIFT_A;
		}

	}

	private class RuleStyleApplication extends StyleApplication {

		private Selector selector;

		public RuleStyleApplication( Selector selector, CSSStyleDeclaration cssBlock ) {
			super(cssBlock);
			this.selector = selector;
		}

		public Selector getSelector() {
			return selector;
		}

		@Override
		public int getSpecificity() {
			return calculateSpecificity( selector );
		}

		private int calculateSpecificity( Selector selector ) {
			switch ( selector.getSelectorType() ) {
				case Selector.SAC_CONDITIONAL_SELECTOR:
					SimpleSelector subSelector = ((ConditionalSelector) selector).getSimpleSelector();
					Condition condition = ((ConditionalSelector) selector).getCondition();
					return calculateSpecificity( subSelector ) + scoreForCondition( condition );
				case Selector.SAC_CHILD_SELECTOR:
				case Selector.SAC_DESCENDANT_SELECTOR:
					DescendantSelector descendantSelector = (DescendantSelector) selector;
					return calculateSpecificity( descendantSelector.getAncestorSelector() ) +
							calculateSpecificity( descendantSelector.getSimpleSelector() );
				case Selector.SAC_ELEMENT_NODE_SELECTOR:
					return 1;
			}
			return 0;
		}

		private int scoreForCondition( Condition condition ) {
			switch ( condition.getConditionType() ) {
				case Condition.SAC_NEGATIVE_CONDITION:
					return scoreForCondition( ((NegativeCondition) condition).getCondition() );
				case Condition.SAC_ID_CONDITION:
					return 1 << SHIFT_B;
				case Condition.SAC_CLASS_CONDITION:
				case Condition.SAC_ATTRIBUTE_CONDITION:
				case Condition.SAC_PSEUDO_CLASS_CONDITION:
					return 1 << SHIFT_C;
				case Condition.SAC_AND_CONDITION:
					CombinatorCondition combinatorCondition = (CombinatorCondition) condition;
					return scoreForCondition( combinatorCondition.getFirstCondition() ) +
							scoreForCondition( combinatorCondition.getSecondCondition() );
				case Condition.SAC_POSITIONAL_CONDITION:
					return 1;
				default:
					return 0;
			}
		}

	}

	public String prettyPrintDocument() {
		final StringBuilder builder = new StringBuilder();
		NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof Document ) {
                    // don't print anything
                } else if ( node instanceof TextNode ) {
                    builder.append( ((TextNode) node).getWholeText() );
                } else if ( node instanceof DocumentType ) {
                    builder.append( node.toString() );
                    builder.append( '\n' );
                } else if ( node instanceof DataNode ) {
                    builder.append( ((DataNode) node).getWholeData() );
                } else if ( node instanceof Element ) {
                    Element element = (Element) node;
                    if ( element.tag().formatAsBlock() ||
                            (element.parent() != null && element.parent().tag().formatAsBlock()) ) {
                    }
                    builder.append( '<' );
                    builder.append( element.tagName() );
                    builder.append( element.attributes().html() );
                    if ( element.tag().isSelfClosing() ) {
                        builder.append( "/>" );
                    } else {
                        builder.append( '>' );
                    }
                } else if ( node instanceof Comment ) {
                    builder.append( "<!--" );
                    builder.append( ((Comment) node).getData() );
                    builder.append( "-->" );
                } else {
                    // TODO: ...?
                    throw new UnsupportedOperationException( "unknown node type: " + node );
                }
            }

            @Override
            public void tail( Node node, int depth ) {
                if ( node instanceof Document ) {
                    // print nothing
                } else if ( node instanceof Element ) {
                    Element element = (Element) node;
                    if ( element.tag().formatAsBlock() ||
                            (element.parent() != null && element.parent().tag().formatAsBlock()) ) {
//                      appendIndent( builder, depth );
                    }
                    if ( !element.tag().isSelfClosing() ) {
                        builder.append( "</" );
                        builder.append( ((Element) node).tagName() );
                        builder.append( '>' );
                    }
                } else {
                    // no other nodes need anything done here
                }
            }
        }, getHtmlDocument() );
		return builder.toString();
	}

	private void appendIndent( StringBuilder builder, int level ) {
		for (int i = 0; i < level; i++) {
			builder.append( INDENT );
		}
	}

	// =================================================================================
	// for testing:

	public static void main( String[] args ) {
		switch ( args.length ) {
			case 0:
				try {
					speedTest();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case 1:
				// apply to a file, print result to STDOUT
				try {
					System.out.println( processFile( new File( args[0] ) ) );
				} catch (IOException e) {
					e.printStackTrace();
				} catch (CSSException e) {
					e.printStackTrace();
				}
				break;
			default:
				System.out.println( "args: <html file>" );
		}
	}


	public static void speedTest() throws IOException {

		System.out.println( "SPEED TEST\n" );

		int NUM_TIMES = 50;

		File htmlFile = new File( "/usr/local/meetup/test_data/com/meetup/util/stylesheetapplier/velocitytest/new_comment_alert.email.extcss.html" );

		System.out.printf( "Processing %s, %d times...\n", htmlFile, NUM_TIMES );

		// run test
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < NUM_TIMES; i++) {
			System.out.printf("time #%d\n", i);
			String result = StylesheetApplier.processFile( htmlFile );
		}
		long endTime = System.currentTimeMillis();
		float elapsedTimeSeconds = ((float)(endTime - startTime))/1000f;

		System.out.printf( "%d iterations in %f seconds: %f seconds per iteration; %f iterations per second\n",
				NUM_TIMES, elapsedTimeSeconds, elapsedTimeSeconds / (float)NUM_TIMES,
				(float)NUM_TIMES / elapsedTimeSeconds );
	}

}