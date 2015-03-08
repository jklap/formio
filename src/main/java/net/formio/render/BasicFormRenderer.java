/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.formio.render;

import java.util.List;

import net.formio.BasicListFormMapping;
import net.formio.Field;
import net.formio.FormElement;
import net.formio.FormField;
import net.formio.FormMapping;
import net.formio.Forms;
import net.formio.ajax.AjaxParams;
import net.formio.ajax.JsEvent;
import net.formio.choice.ChoiceRenderer;
import net.formio.common.MessageTranslator;
import net.formio.props.JsEventToUrl;
import net.formio.validation.ConstraintViolationMessage;

/**
 * <p>Basic implementation of {@link FormRenderer} using Bootstrap markup and styles.
 * <p>You probably want to override the rendered markup to meet your needs - you
 * can create custom subclass that uses your favorite templating system and
 * overrides some or all methods with "renderHtml" prefix.
 * 
 * @author Radek Beran
 */
public class BasicFormRenderer {

	private final RenderContext ctx;
	// Auxiliary renderers
	private final MessageRenderer messageRenderer;
	private final LabelRenderer labelRenderer;
	private final DatePickerRenderer datePickerRenderer;

	public BasicFormRenderer(RenderContext ctx) {
		if (ctx == null) {
			throw new IllegalArgumentException("ctx cannot be null");
		}
		this.ctx = ctx;
		this.messageRenderer = new MessageRenderer(ctx);
		this.labelRenderer = new LabelRenderer(ctx);
		this.datePickerRenderer = new DatePickerRenderer(ctx);
	}

	public <T> String renderGlobalMessages(FormMapping<T> formMapping) {
		return messageRenderer.renderGlobalMessages(formMapping);
	}

	/**
	 * <p>Renders given form element - form mapping or form field:</p>
	 * <ul>
	 * 	<li>Surrounding placeholder tag (even if the element is invisible).</li>
	 * 	<li>Form mapping or form field if it is visible (element markup).</li>
	 * </ul>
	 * <p>Visible mapping consists of mapping box with label and nested elements, 
	 * visible form field consists of field box with label and field envelope (with nested form input).</p>
	 * 
	 * @param element
	 * @return
	 */
	public String renderElement(FormElement<?> element) {
		return renderHtmlElementPlaceholder(element, renderElementMarkup(element));
	}

	/**
	 * Renders the element itself without the surrounding placeholder tag.
	 * If the given element is invisible, returns empty string.
	 * @param element
	 * @return
	 */
	public String renderElementMarkup(FormElement<?> element) {
		StringBuilder sb = new StringBuilder("");
		if (element.isVisible()) {
			sb.append(renderVisibleElement(element));
		}
		return sb.toString();
	}
		
	public String renderVisibleElement(FormElement<?> element) {
		String html = null;
		if (element instanceof FormMapping) {
			html = renderVisibleMapping((FormMapping<?>)element);
		} else if (element instanceof FormField) {
			html = renderVisibleField((FormField<?>)element);
		} else if (element != null) {
			throw new UnsupportedOperationException("Unsupported element " + element.getClass().getName());
		}
		return html;
	}

	public <T> String renderVisibleMapping(FormMapping<T> mapping) {
		StringBuilder sb = new StringBuilder();
		
		// Label
		sb.append(renderMappingLabelElement(mapping));

		// Mapping messages
		sb.append(renderMessageList(mapping));
		
		// Nested mappings and fields
		if (mapping instanceof BasicListFormMapping) {
			for (FormMapping<?> m : ((BasicListFormMapping<?>) mapping).getList()) {
				sb.append(renderElement(m));
			}
		} else {
			for (FormElement<?> el : mapping.getElements()) {
				sb.append(renderElement(el));
			}
		}
		return newLine() + renderHtmlMappingBox(mapping, sb.toString());
	}

	public <T> String renderVisibleField(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		String type = getFieldType(field);
		Field formComponent = Field.findByType(type);
		if (formComponent != null) {
			switch (formComponent) {
			case HIDDEN:
				sb.append(renderFieldHidden(field));
				break;
			case TEXT:
				sb.append(renderFieldText(field));
				break;
			case TEXT_AREA:
				sb.append(renderFieldTextArea(field));
				break;
			case PASSWORD:
				sb.append(renderFieldPassword(field));
				break;
			case CHECK_BOX:
				sb.append(renderFieldCheckBox(field));
				break;
			case DATE_PICKER:
				sb.append(renderFieldDatePicker(field));
				break;
			case DROP_DOWN_CHOICE:
				sb.append(renderFieldDropDownChoice(field));
				break;
			case FILE_UPLOAD:
				sb.append(renderFieldFileUpload(field));
				break;
			case MULTIPLE_CHECK_BOX:
				sb.append(renderFieldMultipleCheckbox(field));
				break;
			case MULTIPLE_CHOICE:
				sb.append(renderFieldMultipleChoice(field));
				break;
			case RADIO_CHOICE:
				sb.append(renderFieldRadioChoice(field));
				break;
			case COLOR:
				sb.append(renderFieldColor(field));
				break;
			case DATE:
				sb.append(renderFieldDate(field));
				break;
			case DATE_TIME:
				sb.append(renderFieldDateTime(field));
				break;
			case DATE_TIME_LOCAL:
				sb.append(renderFieldDateTimeLocal(field));
				break;
			case TIME:
				sb.append(renderFieldTime(field));
				break;
			case EMAIL:
				sb.append(renderFieldEmail(field));
				break;
			case MONTH:
				sb.append(renderFieldMonth(field));
				break;
			case NUMBER:
				sb.append(renderFieldNumber(field));
				break;
			case RANGE:
				sb.append(renderFieldRange(field));
				break;
			case SEARCH:
				sb.append(renderFieldSearch(field));
				break;
			case TEL:
				sb.append(renderFieldTel(field));
				break;
			case URL:
				sb.append(renderFieldUrl(field));
				break;
			case WEEK:
				sb.append(renderFieldWeek(field));
				break;
			case SUBMIT_BUTTON:
				sb.append(renderFieldSubmitButton(field));
				break;
			default:
				throw new UnsupportedOperationException(
						"Cannot render component with type " + type);
			}
		} else {
			throw new UnsupportedOperationException("Unsupported component with type " + type);
		}
		return sb.toString();
	}

	public TdiResponseBuilder ajaxResponse() {
		return createTdiResponseBuilder();
	}
	
	protected TdiResponseBuilder createTdiResponseBuilder() {
		return new TdiResponseBuilder(this);
	}
	
	public String renderElementId(FormElement<?> element) {
		return renderIdForName(element.getName());
	}
	
	public String renderIdForName(String name) {
		return "id" + Forms.PATH_SEP + name;
	}
	
	protected <T> String renderElementIdWithIndex(FormField<T> field, int itemIndex) {
		return renderElementId(field) + Forms.PATH_SEP + itemIndex;
	}
	
	protected String renderElementPlaceholderId(FormElement<?> element) {
		return renderElementPlaceholderId(element.getName());
	}
	
	protected String renderElementPlaceholderId(String elementName) {
		return "placeholder" + Forms.PATH_SEP + elementName;
	}
	
	protected String renderHtmlElementPlaceholder(FormElement<?> element, String innerMarkup) {
		StringBuilder sb = new StringBuilder();
		// Element placeholder begin - rendered even for invisible element so there is reserved
		// identified place that can be updated if the element becomes visible.
		sb.append("<div id=\"" + renderElementPlaceholderId(element) + "\">" + newLine());
		
		// The element itself
		sb.append(innerMarkup);
		
		// Element placeholder end
		sb.append("</div>" + newLine());
		return sb.toString();
	}

	protected <T> String renderHtmlMappingBox(FormMapping<T> mapping, String innerMarkup) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"" + getRenderContext().getMaxSeverityClass(mapping) + "\">" + newLine());
		sb.append(innerMarkup);
		sb.append("</div>" + newLine());
		return sb.toString();
	}
	
	protected <T> String renderHtmlFieldBox(FormField<T> field, String innerMarkup) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"" + getRenderContext().getFormBoxClass() + " " + 
			getRenderContext().getMaxSeverityClass(field) + "\">" + newLine());
		boolean checkbox = isCheckBox(field);
		if (checkbox) {
			sb.append("<div class=\"" + Field.CHECK_BOX.getInputType() + "\">" + newLine());
		}
		
		sb.append(innerMarkup);
		
		if (checkbox) {
			sb.append("</div>" + newLine());
		}
		sb.append("</div>" + newLine() + newLine());
		return sb.toString();
	}

	protected <T> String renderHtmlInputEnvelope(FormField<T> field, String innerMarkup) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"" + getRenderContext().getInputEnvelopeClass(field) + "\">" + newLine());
		sb.append(innerMarkup);
		sb.append("</div>" + newLine());
		return sb.toString();
	}

	protected String renderMessageList(FormElement<?> element) {
		return messageRenderer.renderMessageList(element);
	}

	protected String renderMessage(ConstraintViolationMessage msg) {
		return messageRenderer.renderMessage(msg);
	}

	protected <T> String renderMappingLabelElement(FormMapping<T> mapping) {
		if (mapping.getProperties().isLabelVisible()) {
			return labelRenderer.renderMappingLabelElement(mapping);
		}
		return "";
	}

	protected <T> String renderHtmlLabel(FormElement<?> element) {
		if (element.getProperties().isLabelVisible()) {
			return labelRenderer.renderHtmlLabel(element);
		}
		return "";
	}
	
	protected String renderFieldAttributes(FormElement<?> element) {
		return renderAccessibilityAttributes(element) + renderAjaxAttributes(element);
	}

	protected String renderAccessibilityAttributes(FormElement<?> element) {
		StringBuilder sb = new StringBuilder();
		if (!element.isEnabled()) {
			sb.append(" disabled=\"disabled\"");
		}
		if (element.isReadonly()) {
			sb.append(" readonly=\"readonly\"");
		}
		return sb.toString();
	}

	/**
	 * Renders AJAX attributes of TDI library.
	 * @param ctx
	 * @param element
	 * @return
	 */
	protected String renderAjaxAttributes(FormElement<?> element) {
		StringBuilder sb = new StringBuilder();
		if (element instanceof FormField) {
			FormField<?> field = (FormField<?>)element;
			if (field.getProperties().getDataAjaxUrl() != null && !field.getProperties().getDataAjaxUrl().isEmpty()) {
				sb.append(" data-ajax-url=\"" + field.getProperties().getDataAjaxUrl() + "\"");
			}
			if (field.getProperties().getDataRelatedElement() != null && !field.getProperties().getDataRelatedElement().isEmpty()) {
				sb.append(" data-related-element=\"" + field.getProperties().getDataRelatedElement() + "\"");
			}
			if (field.getProperties().getDataRelatedAncestor() != null && !field.getProperties().getDataRelatedAncestor().isEmpty()) {
				sb.append(" data-related-ancestor=\"" + field.getProperties().getDataRelatedAncestor() + "\"");
			}
			if (field.getProperties().getDataConfirm() != null && !field.getProperties().getDataConfirm().isEmpty()) {
				sb.append(" data-confirm=\"" + field.getProperties().getDataConfirm() + "\"");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Renders content of class attribute.
	 * @param ctx
	 * @param element
	 * @return
	 */
	protected <T> String renderInputClassContent(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		boolean customJsEventsServed = field.getProperties().getDataAjaxEvents() != null && 
			field.getProperties().getDataAjaxEvents().length > 0;
		if (field.getProperties().getDataAjaxUrl() != null && 
			!field.getProperties().getDataAjaxUrl().isEmpty() && 
			!customJsEventsServed) {
			sb.append("tdi");
		}
		if (isFullWidthInput(field)) {
			sb.append(" " + getRenderContext().getFullWidthInputClass());
		}
		return sb.toString();
	}
	
	protected <T> String renderInputPlaceholderValue(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		if (field.getProperties().getPlaceholder() != null) {
			sb.append(" placeholder=\"" + getRenderContext().renderValue(field.getProperties().getPlaceholder()) + "\"");
		}
		return sb.toString();
	}
	
	/**
	 * Renders script for handling form field.
	 * @param field
	 * @param multipleInputs
	 * @return
	 */
	protected <T> String renderFieldScript(FormField<T> field, boolean multipleInputs) {
		StringBuilder sb = new StringBuilder();
		if (field.getProperties().getDataAjaxEvents() != null && field.getProperties().getDataAjaxEvents().length > 0) {
			sb.append("<script>" + newLine());
			if (multipleInputs) {
				if (field.getChoices() != null && field.getChoiceRenderer() != null) {
					List<?> items = field.getChoices().getItems();
					if (items != null) {
						for (int i = 0; i < items.size(); i++) {
							String itemId = renderElementIdWithIndex(field, i);
							sb.append(renderTdiSend(field, itemId, field.getProperties().getDataAjaxEvents()));
						}
					}
				}
			} else {
				sb.append(renderTdiSend(field, renderElementId(field), field.getProperties().getDataAjaxEvents()));
			}
			sb.append("</script>" + newLine());
		}
		return sb.toString();
	}

	protected <T> String renderHtmlTextArea(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		sb.append("<textarea name=\"" + field.getName() + "\" id=\"" + renderElementId(field) + 
			"\" class=\"" + renderInputClassContent(field) + "\"");
		sb.append(renderFieldAttributes(field));
		sb.append(renderInputPlaceholderValue(field));
		sb.append(">");
		sb.append(getRenderContext().renderValue(field.getValue()));
		sb.append("</textarea>" + newLine());
		sb.append(renderFieldScript(field, false));
		return sb.toString();
	}

	protected <T> String renderHtmlInput(FormField<T> field) {
		String typeId = getFieldType(field);
		Field formComponent = Field.findByType(typeId);
		String inputType = formComponent.getInputType();
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=\"" + inputType + "\" name=\"" + field.getName()
				+ "\" id=\"" + renderElementId(field) + "\"");
		if (!Field.FILE_UPLOAD.getType().equals(typeId)) {
			String value = getRenderContext().renderValue(field.getValue());
			sb.append(" value=\"" + value + "\"");
		}
		if (!Field.HIDDEN.getType().equals(typeId)) {
			sb.append(renderFieldAttributes(field));
		}
		sb.append(" class=\"" + renderInputClassContent(field) + "\"");
		sb.append(renderInputPlaceholderValue(field));
		sb.append("/>" + newLine());
		sb.append(renderFieldScript(field, false));
		return sb.toString();
	}

	protected <T> String renderHtmlCheckBox(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=\"checkbox\" name=\"" + field.getName()
				+ "\" id=\"" + renderElementId(field) + "\" value=\""
				+ getRenderContext().renderValue("1") + "\"");
		if (field.getValue() != null && !field.getValue().isEmpty()) {
			String lc = field.getValue().toLowerCase();
			if (Boolean.valueOf(lc.equals("t") || lc.equals("y") || lc.equals("true") || lc.equals("1")).booleanValue()) {
				sb.append(" checked=\"checked\" ");
			}
		}
		sb.append(renderFieldAttributes(field));
		sb.append(" class=\"" + renderInputClassContent(field) + "\"");
		sb.append("/>" + newLine());
		sb.append(renderFieldScript(field, false));
		return sb.toString();
	}

	protected <T> String renderHtmlSelect(FormField<T> field, boolean multiple, Integer size) {
		StringBuilder sb = new StringBuilder();
		sb.append("<select name=\"" + field.getName() + "\" id=\"" + renderElementId(field) + "\"");
		if (multiple) {
			sb.append(" multiple=\"multiple\"");
		}
		if (size != null) {
			sb.append(" size=\"" + size + "\"");
		}
		sb.append(" class=\"" + renderInputClassContent(field) + "\"");
		sb.append(renderFieldAttributes(field));
		sb.append(">" + newLine());
		if (field.getChoices() != null && field.getChoiceRenderer() != null) {
			List<?> items = field.getChoices().getItems();
			if (items != null) {
				// First "Choose One" option
				if (field.getProperties().isChooseOptionDisplayed()) {
					sb.append(renderHtmlOption("", field.getProperties().getChooseOptionTitle(), false));
				}
				ChoiceRenderer<Object> choiceRenderer = getChoiceRenderer(field);
				int itemIndex = 0;
				for (Object item : items) {
					String value = getChoiceValue(choiceRenderer, item, itemIndex);
					String title = getChoiceTitle(choiceRenderer, item, itemIndex);
					boolean selected = field.getFilledObjects().contains(item);
					sb.append(renderHtmlOption(value, title, selected));
					itemIndex++;
				}
			}
		}
		sb.append("</select>" + newLine());
		sb.append(renderFieldScript(field, false));
		return sb.toString();
	}

	protected <T> String renderHtmlChecks(FormField<T> field) {
		String type = Field.RADIO_CHOICE.getType().equals(field.getType()) ? 
			Field.RADIO_CHOICE.getType() : 
			Field.CHECK_BOX.getType();
		StringBuilder sb = new StringBuilder();
		if (field.getChoices() != null && field.getChoiceRenderer() != null) {
			List<?> items = field.getChoices().getItems();
			if (items != null) {
				ChoiceRenderer<Object> choiceRenderer = getChoiceRenderer(field);
				int itemIndex = 0;
				for (Object item : items) {
					String value = getChoiceValue(choiceRenderer, item, itemIndex);
					String title = getChoiceTitle(choiceRenderer, item, itemIndex);
					String itemId = renderElementIdWithIndex(field, itemIndex);

					sb.append("<div class=\"" + type + "\">" + newLine());
					if (field.getProperties().isLabelVisible()) {
						sb.append(renderLabelBeginTag(field));
					}
					
					sb.append("<input type=\"" + type + "\" name=\"" + field.getName() + "\" id=\"" + itemId + "\" value=\"" + value + "\"");
					if (field.getFilledObjects().contains(item)) {
						sb.append(" checked=\"checked\"");
					}
					sb.append(renderFieldAttributes(field));
					sb.append(" class=\"" + renderInputClassContent(field) + "\"");
					sb.append("/>");
					if (field.getProperties().isLabelVisible()) {
						sb.append(" " + title + renderLabelEndTag(field));
					}
					sb.append("</div>" + newLine());
					itemIndex++;
				}
			}
		}
		sb.append(renderFieldScript(field, true));
		return sb.toString();
	}
	
	protected String renderHtmlOption(String value, String title, boolean selected) {
		StringBuilder sb = new StringBuilder();
		sb.append("<option value=\"" + getRenderContext().escapeHtml(value) + "\"");
		if (selected) {
			sb.append(" selected=\"selected\"");
		}
		sb.append(">" + getRenderContext().escapeHtml(title) + "</option>" + newLine());
		return sb.toString();
	}
	
	protected <T> String renderHtmlButton(FormField<T> field) {
		StringBuilder sb = new StringBuilder();
		sb.append("<button type=\"submit\" value=\"" + getRenderContext().renderValue(field.getValue()) + "\" class=\"btn btn-default\">");
		MessageTranslator tr = getRenderContext().createMessageTranslator(field);
		String text = getRenderContext().escapeHtml(tr.getMessage(field.getLabelKey()));
		sb.append(text);
		sb.append("</button>" + newLine());
		return sb.toString();
	}

	protected <T> String renderDatePickerJavaScript(FormField<T> field) {
		return datePickerRenderer.renderDatePickerJavaScript(field);
	}

	protected <T> String renderTextFieldInternal(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlInput(field) + 
				renderMessageList(field)));
	}

	// --- Various field types - begin ---

	protected <T> String renderFieldSubmitButton(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlInputEnvelope(field, 
				renderHtmlButton(field)));
	}

	protected <T> String renderFieldHidden(FormField<T> field) {
		return renderHtmlInput(field) + newLine();
	}

	protected <T> String renderFieldText(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldColor(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldDate(FormField<T> field) {
		// TODO: Support for min, max attributes
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldDateTime(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldDateTimeLocal(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldTime(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldEmail(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldMonth(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldNumber(FormField<T> field) {
		// TODO: Support for min, max, step attributes
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldRange(FormField<T> field) {
		// TODO: Support for min, max attributes
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldSearch(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldTel(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldUrl(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldWeek(FormField<T> field) {
		return renderTextFieldInternal(field);
	}

	protected <T> String renderFieldTextArea(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlTextArea(field) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldCheckBox(FormField<T> field) {
		return renderHtmlFieldBox(field,
			renderHtmlInputEnvelope(field,
				renderLabelBeginTag(field) +
				renderHtmlCheckBox(field) + 
				renderLabelText(field) +
				renderLabelEndTag(field) +
				renderMessageList(field))
		);
	}

	protected <T> String renderFieldPassword(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlInput(field) +
				renderMessageList(field)));
	}

	protected <T> String renderFieldFileUpload(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlInput(field) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldDatePicker(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlInput(field) + 
				renderDatePickerJavaScript(field) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldDropDownChoice(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlSelect(field, false, null) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldMultipleChoice(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlSelect(field, true, null) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldMultipleCheckbox(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlChecks(field) + 
				renderMessageList(field)));
	}

	protected <T> String renderFieldRadioChoice(FormField<T> field) {
		return renderHtmlFieldBox(field, 
			renderHtmlLabel(field) + 
			renderHtmlInputEnvelope(field, 
				renderHtmlChecks(field) + 
				renderMessageList(field)));
	}

	// --- /Various field types - end ---

	protected <T> String renderLabelBeginTag(FormElement<?> formElement) {
		if (formElement.getProperties().isLabelVisible()) {
			return labelRenderer.renderLabelBeginTag(formElement);
		}
		return "";
	}

	protected <T> String renderLabelEndTag(FormElement<?> formElement) {
		if (formElement.getProperties().isLabelVisible()) {
			return labelRenderer.renderLabelEndTag(formElement);
		}
		return "";
	}

	protected <T> String renderLabelText(FormElement<?> formElement) {
		if (formElement.getProperties().isLabelVisible()) {
			return labelRenderer.renderLabelText(formElement);
		}
		return "";
	}

	protected <T> String renderRequiredMark(FormElement<?> formElement) {
		return labelRenderer.renderRequiredMark(formElement);
	}

	protected RenderContext getRenderContext() {
		return ctx;
	}

	private <T> boolean isFullWidthInput(FormField<T> field) {
		String type = getFieldType(field);
		Field fld = Field.findByType(type);
		return !type.equals(Field.FILE_UPLOAD.getType()) // otherwise border around field with "Browse" text is drawn
			&& !type.equals(Field.HIDDEN.getType())
			&& !type.equals(Field.CHECK_BOX.getType())
			&& (fld == null || !Field.withMultipleInputs.contains(fld));
	}

	private <T> String getFieldType(FormField<T> field) {
		String type = field.getType();
		if (type == null) {
			type = Field.TEXT.getType();
		}
		return type.toLowerCase();
	}
	
	private <T> boolean isCheckBox(FormField<T> field) {
		return Field.CHECK_BOX.getType().equals(field.getType());
	}
	
	@SuppressWarnings("unchecked")
	private ChoiceRenderer<Object> getChoiceRenderer(FormField<?> field) {
		return (ChoiceRenderer<Object>)field.getChoiceRenderer();
	}
	
	private String newLine() {
		return getRenderContext().newLine();
	}
	
	private String getChoiceTitle(ChoiceRenderer<Object> choiceRenderer, Object item, int itemIndex) {
		return getRenderContext().escapeHtml(choiceRenderer.getItem(item, itemIndex).getTitle());
	}

	private String getChoiceValue(ChoiceRenderer<Object> choiceRenderer, Object item, int itemIndex) {
		return getRenderContext().renderValue(choiceRenderer.getItem(item, itemIndex).getId());
	}
	
	/**
	 * Composes JavaScript for given form field that initiates TDI AJAX request when
	 * some given event occurs - different JavaScript events can have different URL addresses
	 * for handling the AJAX request. The value of form field is part of the AJAX request
	 * (if some value is filled).
	 * @param formField
	 * @param inputId
	 * @param events
	 * @return
	 */
	private <T> String renderTdiSend(FormField<T> formField, String inputId, JsEventToUrl[] events) {
		StringBuilder sb = new StringBuilder();
		if (events != null && events.length > 0) {
			String elm = "$(\"#" + inputId + "\")";
			sb.append(elm + ".on({" + newLine());
			for (int i = 0; i < events.length; i++) {
				JsEventToUrl eventToUrl = events[i];
				JsEvent eventType = eventToUrl.getEvent();
				String url = eventToUrl.getUrl();
				if (url == null || url.isEmpty()) {
					url = formField.getProperties().getDataAjaxUrl();
				}
				if (url == null || url.isEmpty()) {
					throw new IllegalArgumentException("No URL for AJAX request is specified");
				}
				url = urlWithAppendedParameter(url, AjaxParams.SRC_ELEMENT_NAME, formField.getName());
				sb.append(eventType.getEventName() + ": function(evt) {"  + newLine());
				// Remember previous data-ajax-url (to revert it back) and set it temporarily to custom URL
				sb.append("var prevUrl = " + elm + ".attr(\"data-ajax-url\");" + newLine());
				sb.append(elm + ".attr(\"data-ajax-url\", \"" + url + "\");" + newLine());
				sb.append("TDI.Ajax.send(" + elm + ");" + newLine());
				sb.append(elm + ".attr(\"data-ajax-url\", prevUrl);" + newLine());
				sb.append("var prevUrl = null;" + newLine());
				sb.append("}");
				if (i < events.length - 1) {
					// not the last event handler
					sb.append(",");
				}
				sb.append(newLine());
			}
			sb.append("});" + newLine());
		}
		return sb.toString();
	}

	private String urlWithAppendedParameter(String url, String paramName, String paramValue) {
		if (url == null || url.isEmpty()) return null;
		if (url.contains("?") && !url.endsWith("?")) {
			if (!url.endsWith("&")) {
				url = url + "&";
			}
		} else if (!url.endsWith("?")) {
			url = url + "?"; 
		}
		url = url + paramName + "=" + paramValue;
		return url;
	}
}
