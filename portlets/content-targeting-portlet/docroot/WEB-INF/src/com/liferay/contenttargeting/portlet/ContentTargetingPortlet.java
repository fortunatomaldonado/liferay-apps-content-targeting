/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.contenttargeting.portlet;

import com.liferay.contenttargeting.api.model.Rule;
import com.liferay.contenttargeting.api.model.RulesRegistry;
import com.liferay.contenttargeting.model.Campaign;
import com.liferay.contenttargeting.model.RuleInstance;
import com.liferay.contenttargeting.model.UserSegment;
import com.liferay.contenttargeting.portlet.util.UnavailableServiceException;
import com.liferay.contenttargeting.service.CampaignLocalService;
import com.liferay.contenttargeting.service.CampaignService;
import com.liferay.contenttargeting.service.RuleInstanceLocalService;
import com.liferay.contenttargeting.service.RuleInstanceService;
import com.liferay.contenttargeting.service.UserSegmentLocalService;
import com.liferay.contenttargeting.service.UserSegmentService;
import com.liferay.osgi.util.OsgiServiceUnavailableException;
import com.liferay.osgi.util.ServiceTrackerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.util.DateFormatFactory;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;

import freemarker.ext.beans.BeansWrapper;

import freemarker.template.TemplateHashModel;

import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.UnavailableException;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Eduardo Garcia
 * @author Carlos Sierra Andrés
 */
public class ContentTargetingPortlet extends CTFreeMarkerPortlet {

	public void deleteCampaign(ActionRequest request, ActionResponse response)
		throws Exception {

		long campaignId = ParamUtil.getLong(request, "campaignId");

		try {
			_campaignService.deleteCampaign(campaignId);

			sendRedirect(request, response);
		}
		catch (Exception e) {
			SessionErrors.add(request, e.getClass().getName());

			response.setRenderParameter("mvcPath", ContentTargetingPath.ERROR);
		}
	}

	public void deleteRuleInstance(
			ActionRequest request, ActionResponse response)
		throws Exception {

		long ruleInstanceId = ParamUtil.getLong(request, "ruleInstanceId");

		try {
			_ruleInstanceService.deleteRuleInstance(ruleInstanceId);

			sendRedirect(request, response);
		}
		catch (Exception e) {
			SessionErrors.add(request, e.getClass().getName());

			response.setRenderParameter("mvcPath", ContentTargetingPath.ERROR);
		}
	}

	public void deleteUserSegment(
			ActionRequest request, ActionResponse response)
		throws Exception {

		long userSegmentId = ParamUtil.getLong(request, "userSegmentId");

		try {
			_userSegmentService.deleteUserSegment(userSegmentId);

			sendRedirect(request, response);
		}
		catch (Exception e) {
			SessionErrors.add(request, e.getClass().getName());

			response.setRenderParameter("mvcPath", ContentTargetingPath.ERROR);
		}
	}

	@Override
	public void init() throws PortletException {
		super.init();

		Bundle bundle = FrameworkUtil.getBundle(getClass());

		if (bundle == null) {
			throw new UnavailableException(
				"Can't find a reference to the OSGi bundle") {

				@Override
				public boolean isPermanent() {
					return true;
				}
			};
		}

		try {
			_campaignLocalService = ServiceTrackerUtil.getService(
				CampaignLocalService.class, bundle.getBundleContext());
			_campaignService = ServiceTrackerUtil.getService(
				CampaignService.class, bundle.getBundleContext());
			_ruleInstanceLocalService = ServiceTrackerUtil.getService(
				RuleInstanceLocalService.class, bundle.getBundleContext());
			_ruleInstanceService = ServiceTrackerUtil.getService(
				RuleInstanceService.class, bundle.getBundleContext());
			_rulesRegistry = ServiceTrackerUtil.getService(
				RulesRegistry.class, bundle.getBundleContext());
			_userSegmentLocalService = ServiceTrackerUtil.getService(
				UserSegmentLocalService.class, bundle.getBundleContext());
			_userSegmentService = ServiceTrackerUtil.getService(
				UserSegmentService.class, bundle.getBundleContext());
		}
		catch (OsgiServiceUnavailableException osue) {
			throw new UnavailableServiceException(
				osue.getUnavailableServiceClass());
		}
	}

	public void updateRuleInstance(
			ActionRequest request, ActionResponse response)
		throws Exception {

		long ruleInstanceId = ParamUtil.getLong(request, "ruleInstanceId");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			RuleInstance.class.getName(), request);

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		String ruleKey = ParamUtil.getString(request, "ruleKey");

		Rule rule = _rulesRegistry.getRule(ruleKey);

		String typeSettings = rule.processRule(request, response);

		long userSegmentId = ParamUtil.getLong(request, "userSegmentId");

		try {
			if (ruleInstanceId > 0) {
				_ruleInstanceService.updateRuleInstance(
						ruleInstanceId, typeSettings, serviceContext);
			}
			else {
				_ruleInstanceService.addRuleInstance(
						themeDisplay.getUserId(), ruleKey, userSegmentId,
						typeSettings, serviceContext);
			}

			String portletId = PortalUtil.getPortletId(request);

			SessionMessages.add(
				request, portletId + SessionMessages.KEY_SUFFIX_REFRESH_PORTLET,
				portletId);

			sendRedirect(request, response);
		}
		catch (Exception e) {
			SessionErrors.add(request, e.getClass().getName());

			if (e instanceof PrincipalException) {
				response.setRenderParameter(
					"mvcPath", ContentTargetingPath.EDIT_USER_SEGMENT);
			}
			else {
				response.setRenderParameter(
					"mvcPath", ContentTargetingPath.ERROR);
			}
		}
	}

	public void updateUserSegment(
			ActionRequest request, ActionResponse response)
		throws Exception {

		long userSegmentId = ParamUtil.getLong(request, "userSegmentId");

		Map<Locale, String> nameMap = LocalizationUtil.getLocalizationMap(
			request, "name");
		Map<Locale, String> descriptionMap =
			LocalizationUtil.getLocalizationMap(request, "description");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			UserSegment.class.getName(), request);

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		try {
			if (userSegmentId > 0) {
				_userSegmentService.updateUserSegment(
					userSegmentId, nameMap, descriptionMap, serviceContext);
			}
			else {
				_userSegmentService.addUserSegment(
					themeDisplay.getUserId(), nameMap, descriptionMap,
					serviceContext);
			}

			sendRedirect(request, response);
		}
		catch (Exception e) {
			SessionErrors.add(request, e.getClass().getName());

			if (e instanceof PrincipalException) {
				response.setRenderParameter(
					"mvcPath", ContentTargetingPath.EDIT_USER_SEGMENT);
			}
			else {
				response.setRenderParameter(
					"mvcPath", ContentTargetingPath.ERROR);
			}
		}
	}

	protected void populateContext(
			String path, PortletRequest portletRequest,
			PortletResponse portletResponse, Template template)
		throws Exception {

		BeansWrapper wrapper = BeansWrapper.getDefaultInstance();

		TemplateHashModel staticModels = wrapper.getStaticModels();

		template.put("campaignClass", Campaign.class);
		template.put(
			"contentTargetingPath",
			staticModels.get(
				"com.liferay.contenttargeting.portlet.ContentTargetingPath"));
		template.put("currentURL", PortalUtil.getCurrentURL(portletRequest));
		template.put("liferayWindowStatePopUp", LiferayWindowState.POP_UP);
		template.put("portletContext", getPortletContext());
		template.put(
			"redirect", ParamUtil.getString(portletRequest, "redirect"));
		template.put(
			"tabs1", ParamUtil.getString(portletRequest, "tabs1", "campaigns"));
		template.put(
			"userInfo", portletRequest.getAttribute(PortletRequest.USER_INFO));
		template.put("userSegmentClass", UserSegment.class);

		populateViewContext(
			path, portletRequest, portletResponse, template, staticModels);
	}

	protected void populateViewContext(
			String path, PortletRequest portletRequest,
			PortletResponse portletResponse, Template template,
			TemplateHashModel staticModels)
		throws Exception {

		if (Validator.isNull(path) || path.equals(ContentTargetingPath.VIEW)) {
			template.put(
				"actionKeys",
				staticModels.get(
					"com.liferay.contenttargeting.util.ActionKeys"));

			template.put(
				"campaignPermission",
				staticModels.get(
					"com.liferay.contenttargeting.service.permission." +
						"CampaignPermission"));
			template.put(
				"contentTargetingPermission",
				staticModels.get(
					"com.liferay.contenttargeting.service.permission." +
						"ContentTargetingPermission"));
			template.put(
				"userSegmentPermission",
				staticModels.get(
					"com.liferay.contenttargeting.service.permission." +
						"UserSegmentPermission"));

			ThemeDisplay themeDisplay =
				(ThemeDisplay)portletRequest.getAttribute(
					WebKeys.THEME_DISPLAY);

			List<Campaign> campaigns = _campaignService.getCampaigns(
				themeDisplay.getScopeGroupId());

			template.put("campaigns", campaigns);

			List<UserSegment> userSegments =
				_userSegmentService.getUserSegments(
					themeDisplay.getScopeGroupId());

			template.put("userSegments", userSegments);

			Format displayFormatDate =
				FastDateFormatFactoryUtil.getSimpleDateFormat(
					"yyyy-MM-dd HH:mm", themeDisplay.getLocale(),
					themeDisplay.getTimeZone());

			template.put("displayFormatDate", displayFormatDate);
		}
		else if (path.equals(ContentTargetingPath.EDIT_RULE_INSTANCE)) {
			template.put("ruleInstanceClass", RuleInstance.class);

			RuleInstance ruleInstance = null;

			long ruleInstanceId = ParamUtil.getLong(
				portletRequest, "ruleInstanceId");

			String ruleKey;

			if (ruleInstanceId > 0) {
				ruleInstance = _ruleInstanceLocalService.getRuleInstance(
					ruleInstanceId);

				ruleKey = ruleInstance.getRuleKey();

				template.put("ruleInstance", ruleInstance);
			}
			else {
				ruleKey = ParamUtil.getString(portletRequest, "ruleKey");
			}

			Rule rule = _rulesRegistry.getRule(ruleKey);

			String ruleFormHTML = rule.getFormHTML(
				ruleInstance, _cloneTemplateContext(template));

			template.put("ruleFormHTML", ruleFormHTML);
			template.put("ruleInstanceId", ruleInstanceId);
			template.put("ruleKey", ruleKey);
			template.put(
				"userSegmentId",
				ParamUtil.getLong(portletRequest, "userSegmentId"));
		}
		else if (path.equals(
					ContentTargetingPath.EDIT_RULE_INSTANCE_REDIRECT)) {

			String ruleKey = ParamUtil.getString(portletRequest, "ruleKey");

			template.put("ruleKey", ruleKey);
		}
		else if (path.equals(ContentTargetingPath.EDIT_USER_SEGMENT)) {
			long userSegmentId = ParamUtil.getLong(
				portletRequest, "userSegmentId");

			template.put("userSegmentId", userSegmentId);

			if (userSegmentId > 0) {
				template.put(
					"userSegment",
					_userSegmentLocalService.getUserSegment(userSegmentId));
			}
		}
		else if (path.equals(ContentTargetingPath.MANAGE_RULES)) {
			template.put("rulesRegistry", _rulesRegistry);

			Map<String, Rule> rules = _rulesRegistry.getRules();

			template.put("rules", rules.values());

			long userSegmentId = ParamUtil.getLong(
				portletRequest, "userSegmentId");

			if (userSegmentId > 0) {
				List<RuleInstance> ruleInstances =
					_ruleInstanceService.getRuleInstances(userSegmentId);

				template.put("ruleInstances", ruleInstances);

				UserSegment userSegment =
					_userSegmentLocalService.getUserSegment(userSegmentId);

				template.put("userSegment", userSegment);
			}
		}
	}

	private Map<String, Object> _cloneTemplateContext(Template template) {
		Map<String, Object> context = new HashMap<String, Object>();

		for (String key : template.getKeys()) {
			context.put(key, template.get(key));
		}

		return context;
	}

	private static Log _log = LogFactoryUtil.getLog(
		ContentTargetingPortlet.class);

	private CampaignLocalService _campaignLocalService;
	private CampaignService _campaignService;
	private RuleInstanceLocalService _ruleInstanceLocalService;
	private RuleInstanceService _ruleInstanceService;
	private RulesRegistry _rulesRegistry;
	private UserSegmentLocalService _userSegmentLocalService;
	private UserSegmentService _userSegmentService;

}