package com.randomnoun.build.javaToGraphviz;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.stylesheets.MediaList;
import org.w3c.dom.stylesheets.StyleSheet;

import com.steadystate.css.dom.CSSRuleListImpl;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.format.CSSFormat;
import com.steadystate.css.parser.CSSOMParser;

// a CSS stylesheet that can import css files from the classpath
// it also removes @media selectors as the jsoup selector impl doesn't use them. Probably.

public class RestrictedCssStyleSheetImpl extends CSSStyleSheetImpl {

    Logger logger = Logger.getLogger(RestrictedCssStyleSheetImpl.class);
    
    CSSStyleSheetImpl wrapped;
    
    public RestrictedCssStyleSheetImpl(CSSStyleSheetImpl wrapped) {
        this.wrapped = wrapped;
    }
    
    
    /** Generated serialVersionUID */
    private static final long serialVersionUID = 1199806416222789849L;

    /**
     * Imports referenced CSSStyleSheets.
     *
     * @param recursive <code>true</code> if the import should be done
     *   recursively, <code>false</code> otherwise
     */
    public void importImports(final boolean recursive) throws DOMException {
        for (int i = 0; i < wrapped.getCssRules().getLength(); i++) {
            final CSSRule cssRule = wrapped.getCssRules().item(i);
            if (cssRule.getType() == CSSRule.IMPORT_RULE) {
                final CSSImportRule cssImportRule = (CSSImportRule) cssRule;
                try {
                    // try the classpath first
                    String href = cssImportRule.getHref();
                    InputStream is = this.getClass().getResourceAsStream(href);
                    if (is == null && !href.startsWith("/")) {
                        is = this.getClass().getResourceAsStream("/" + href);
                    }
                    if (is != null) {
                        URI cpUri = new URI("classpath", "href", null); // not a real protocol
                        Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);
                        final CSSStyleSheetImpl importedCSS = (CSSStyleSheetImpl)
                            new CSSOMParser().parseStyleSheet(new InputSource(r), null, cpUri.toString());
                        RestrictedCssStyleSheetImpl restrictedImportedCss = new RestrictedCssStyleSheetImpl(importedCSS);
                        if (recursive) {
                            restrictedImportedCss.importImports(recursive);
                        }    
                        
                        /* ignoring medialists
                        final MediaList mediaList = cssImportRule.getMedia();
                        if (mediaList.getLength() == 0) {
                            mediaList.appendMedium("all");
                        }
                        final CSSMediaRuleImpl cssMediaRule = new CSSMediaRuleImpl(wrapped, null, mediaList);  
                        cssMediaRule.setRuleList((CSSRuleListImpl) restrictedImportedCss.getCssRules());
                        wrapped.deleteRule(i);
                        ((CSSRuleListImpl) wrapped.getCssRules()).insert(cssMediaRule, i);
                        */
                        
                        wrapped.deleteRule(i);
                        for (int j = 0; j < restrictedImportedCss.getCssRules().getLength(); j++) {
                            CSSRule rule = restrictedImportedCss.getCssRules().item(j);
                            ((CSSRuleListImpl) wrapped.getCssRules()).insert(rule, i + j);    
                        }
                        i = i + restrictedImportedCss.getCssRules().getLength();
                    
                    } else {
                        logger.warn("Missing style reference '" + href + "'");
                    }

                } catch (final URISyntaxException e) {
                    throw new DOMException(DOMException.SYNTAX_ERR, e.getLocalizedMessage());
                }
                
                
                catch (final IOException e) {
                    // TODO handle exception
                }
            }
        }
    }

    public void setMedia(MediaList media) {
        wrapped.setMedia(media);
    }

    public void setBaseUri(String baseUri) {
        wrapped.setBaseUri(baseUri);
    }

    public String getType() {
        return wrapped.getType();
    }

    public boolean getDisabled() {
        return wrapped.getDisabled();
    }

    public void setDisabled(boolean disabled) {
        wrapped.setDisabled(disabled);
    }

    public Node getOwnerNode() {
        return wrapped.getOwnerNode();
    }

    public StyleSheet getParentStyleSheet() {
        return wrapped.getParentStyleSheet();
    }

    public String getHref() {
        return wrapped.getHref();
    }

    public String getTitle() {
        return wrapped.getTitle();
    }

    public MediaList getMedia() {
        return wrapped.getMedia();
    }

    public CSSRule getOwnerRule() {
        return wrapped.getOwnerRule();
    }

    public CSSRuleList getCssRules() {
        return wrapped.getCssRules();
    }

    public int insertRule(String rule, int index) throws DOMException {
        return wrapped.insertRule(rule, index);
    }

    public void deleteRule(int index) throws DOMException {
        wrapped.deleteRule(index);
    }

    public boolean isReadOnly() {
        return wrapped.isReadOnly();
    }

    public void setReadOnly(boolean b) {
        wrapped.setReadOnly(b);
    }

    public void setOwnerNode(Node ownerNode) {
        wrapped.setOwnerNode(ownerNode);
    }

    public void setParentStyleSheet(StyleSheet parentStyleSheet) {
        wrapped.setParentStyleSheet(parentStyleSheet);
    }

    public void setHref(String href) {
        wrapped.setHref(href);
    }

    public void setTitle(String title) {
        wrapped.setTitle(title);
    }

    public void setMediaText(String mediaText) {
        wrapped.setMediaText(mediaText);
    }

    public void setOwnerRule(CSSRule ownerRule) {
        wrapped.setOwnerRule(ownerRule);
    }

    public void setCssRules(CSSRuleList rules) {
        wrapped.setCssRules(rules);
    }

    public String getCssText() {
        return wrapped.getCssText();
    }

    public String getCssText(CSSFormat format) {
        return wrapped.getCssText(format);
    }

    public String toString() {
        return wrapped.toString();
    }

    public boolean equals(Object obj) {
        return wrapped.equals(obj);
    }

    public int hashCode() {
        return wrapped.hashCode();
    }
    
}
