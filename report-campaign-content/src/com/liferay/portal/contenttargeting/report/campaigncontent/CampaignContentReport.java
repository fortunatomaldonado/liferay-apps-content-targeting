/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.portal.contenttargeting.report.campaigncontent;

import com.liferay.portal.contenttargeting.api.model.BaseReport;
import com.liferay.portal.contenttargeting.api.model.Report;
import com.liferay.portal.contenttargeting.model.Campaign;
import com.liferay.portal.contenttargeting.report.campaigncontent.model.CampaignContent;
import com.liferay.portal.contenttargeting.report.campaigncontent.service.CampaignContentLocalServiceUtil;
import com.liferay.portal.contenttargeting.report.campaigncontent.util.comparator.CampaignContentCountComparator;
import com.liferay.portal.contenttargeting.util.SearchContainerIterator;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringPool;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Eduardo Garcia
 */
@Component(immediate = true, service = Report.class)
public class CampaignContentReport extends BaseReport {

	@Activate
	@Override
	public void activate() {
		super.activate();
	}

	@Deactivate
	@Override
	public void deActivate() {
		super.deActivate();
	}

	@Override
	public String getIcon() {
		return "icon-eye-open";
	}

	@Override
	public String getReportType() {
		return Campaign.class.getName();
	}

	@Override
	public String updateReport(long classPK) {
		try {
			CampaignContentLocalServiceUtil.checkCampaignContentEvents(classPK);
		}
		catch (Exception e) {
			_log.error("Cannot update report", e);
		}

		return StringPool.BLANK;
	}

	@Override
	protected void populateContext(Map<String, Object> context) {
		final long classPK = MapUtil.getLong(context, "classPK", 0);

		context.put(
			"searchContainerIterator",
			new SearchContainerIterator<CampaignContent>() {

				@Override
				public List<CampaignContent> getResults(int start, int end)
					throws PortalException, SystemException {

					return CampaignContentLocalServiceUtil.getCampaignContents(
						classPK, start, end,
						new CampaignContentCountComparator());
				}

				@Override
				public int getTotal() throws PortalException, SystemException {
					return
						CampaignContentLocalServiceUtil.
							getCampaignContentsCount(classPK);
				}
			}
		);
	}

	private static Log _log = LogFactoryUtil.getLog(
		CampaignContentReport.class);

}