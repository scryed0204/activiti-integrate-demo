package com.liam.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.workflow.simple.converter.WorkflowDefinitionConversion;
import org.activiti.workflow.simple.converter.WorkflowDefinitionConversionFactory;
import org.activiti.workflow.simple.converter.json.SimpleWorkflowJsonConverter;
import org.activiti.workflow.simple.definition.WorkflowDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/model/")
public class ActivitiModelController {
	@Autowired
	private RepositoryService repositoryService;

	/**
	 * Activiti Modeler功能畫面進入點，進入時同時新增空白Model
	 * @param request
	 * @param response
	 */
	@RequestMapping("/new")
	public void createModel(HttpServletRequest request, HttpServletResponse response) {
		String defaultModelName = "ModelName"; //Model初始化預設名稱
		ObjectMapper objectMapper = new ObjectMapper();

		try {			
			// 初始化Model
			Model modelData = repositoryService.newModel();
			ObjectNode modelObjectNode = objectMapper.createObjectNode();
			modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, defaultModelName);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, "description");
			modelData.setMetaInfo(modelObjectNode.toString());
			modelData.setName(defaultModelName); 
			
			// 將Model資訊到ACT_RE_MODEL，取得Model ID
			repositoryService.saveModel(modelData);
			
			// 初始化Model對應之流程圖資訊(空白)
			ObjectNode editorNode = objectMapper.createObjectNode();
			ObjectNode stencilSetNode = objectMapper.createObjectNode();
			stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
			editorNode.put("id", "canvas");
			editorNode.put("resourceId", "canvas");
			editorNode.set("stencilset", stencilSetNode);

			// 將流程圖資訊存入ACT_GE_BYTEARRAY
			repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
			
			// 進入Activiti Modeler功能畫面
			response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelData.getId());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/edit/{modelId}", method = RequestMethod.GET)
	public void editModel(@PathVariable String modelId, HttpServletRequest request, HttpServletResponse response) {
		try {
			// 進入Activiti Modeler功能畫面
			response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/delete/{modelId}", method = RequestMethod.GET)
	public void deleteModel(@PathVariable String modelId, HttpServletRequest request, HttpServletResponse response) {
		try {
			//透過repositoryService刪除Activiti Model
			repositoryService.deleteModel(modelId);
			response.sendRedirect(request.getContextPath() + "/processes");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/export/{modelId}", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadFile(@PathVariable String modelId, HttpServletRequest request) {

		String fileName = null;
		byte[] bpmnBytes = null;
		// Load file as Resource
		Resource resource = null;
		// Content Type = XML
		String contentType = "application/xml";

		try {
			Model modelData = repositoryService.getModel(modelId);
			if (null != modelData) {
				if ("table-editor".equals(modelData.getCategory())) {
					byte[] modelSource = repositoryService.getModelEditorSource(modelId);

					SimpleWorkflowJsonConverter converter = new SimpleWorkflowJsonConverter();
					WorkflowDefinitionConversionFactory conversionFactory = new WorkflowDefinitionConversionFactory();
					WorkflowDefinition workflowDefinition = converter.readWorkflowDefinition(modelSource);
					fileName = workflowDefinition.getName();
					WorkflowDefinitionConversion conversion = conversionFactory
							.createWorkflowDefinitionConversion(workflowDefinition);
					conversion.convert();
					bpmnBytes = conversion.getBpmn20Xml().getBytes("utf-8");

				} else {
					JsonNode editorNode = new ObjectMapper()
							.readTree(repositoryService.getModelEditorSource(modelData.getId()));
					BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
					BpmnModel model = jsonConverter.convertToBpmnModel(editorNode);
					fileName = model.getMainProcess().getId() + ".bpmn20.xml";
					bpmnBytes = new BpmnXMLConverter().convertToXML(model);
				}
			}

			ByteArrayInputStream in = new ByteArrayInputStream(bpmnBytes);
			resource = new InputStreamResource(in);
		} catch (IOException ex) {
			// logger.info("Could not determine file type.");
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").body(resource);
	}
}
