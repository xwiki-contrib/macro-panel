/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.ow2.xwiki.macro.panel.internal;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.ow2.xwiki.macro.panel.PanelMacroParameters;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * This is a really simple java macro to replace the old velocity based #panel...
 */
@Component
@Named("panel")
public class PanelMacro extends AbstractMacro<PanelMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Panel Macro";

    @Inject
    private ComponentManager manager;
    
    @Inject
    private Execution execution;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public PanelMacro()
    {
        super("Panel", DESCRIPTION, PanelMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(PanelMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException {

        // Build structure is the following:
        // panel (group)
        // * title (header)
        // * content (group)
        // ** [any parsed content] (document->first child)

        Block title = new WordBlock(parameters.getTitle());
        Block header = new HeaderBlock(Collections.singletonList(title), HeaderLevel.LEVEL1);
        header.setParameter("class", "xwikipaneltitle");

        // FIXME is a space the right thing to place in the group when there is no content ?
        Block xdom = new SpaceBlock();
        if (content != null) {
            try {

                // FIXME to be replaced by MacroContentParser when available
                Parser parser = manager.lookup(Parser.class, context.getSyntax().toIdString());
                xdom = parser.parse(new StringReader(content));
                xdom = xdom.getChildren().iterator().next();

            } catch (ComponentLookupException e) {
                throw new MacroExecutionException("Cannot find the XWiki 2.0 Parser", e);
            } catch (ParseException e) {
                throw new MacroExecutionException("Cannot parse the given content", e);
            }
        }

        GroupBlock contentGroup = new GroupBlock(Collections.singletonList(xdom));
        contentGroup.setParameter("class", "xwikipanelcontents");

        String name = findPageName();
        String classAttribute = "panel expanded";
        if (name != null) {
            // If name is found, escape the value and add it as a class attribute
            // This allows per panel style customization
            classAttribute += " ";
            classAttribute += StringEscapeUtils.escapeHtml(name);
        }
        Block panelGroup = new GroupBlock(Arrays.asList(header, contentGroup));
        panelGroup.setParameter("class", classAttribute);

        return Collections.singletonList(panelGroup);

    }

    /**
     * This is a bit of a hack to find the page "hosting" this panel definition.
     * @return the panel page name
     */
    private String findPageName() {

        DocumentModelBridge page = null;
        try {
            ExecutionContext executionContext = this.execution.getContext();
            Map<String, Object> xwikiContext = (Map<String, Object>) executionContext.getProperty("xwikicontext");
            VelocityContext velocityContext = (VelocityContext) xwikiContext.get("vcontext");
            page = (DocumentModelBridge) velocityContext.get("paneldoc");
        } catch (Throwable e) {
            // TODO log an error
            return null;
        }

        return page.getDocumentReference().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }
}
