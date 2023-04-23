/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrextras.reporting.cohort.definition.evaluator.rri;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.rri.MissedInfantProphylaxisCohortDefinition;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.evaluator.CohortDefinitionEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Evaluator for MCH mothers who missed infant prophylaxis for baby
 */
@Handler(supports = { MissedInfantProphylaxisCohortDefinition.class })
public class MissedInfantProphylaxisCohortDefinitionEvaluator implements CohortDefinitionEvaluator {
	
	private final Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	EvaluationService evaluationService;
	
	@Override
	public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {
		
		MissedInfantProphylaxisCohortDefinition definition = (MissedInfantProphylaxisCohortDefinition) cohortDefinition;
		
		if (definition == null)
			return null;
		
		Cohort newCohort = new Cohort();
		
		String qry = "select c.patient_id\n"
		        + "from (select e.patient_id,\n"
		        + "             max(e.visit_date)                                as latest_mch_enrollment,\n"
		        + "             mid(max(concat(e.visit_date, e.hiv_status)), 11) as hiv_status_at_enrollment,\n"
		        + "             a.patient_id                                     as anc_patient,\n"
		        + "             a.latest_anc_visit_date,\n"
		        + "             a.latest_anc_test_results,\n"
		        + "             a.nvp_azt as nvp_azt_at_anc,\n"
		        + "             d.patient_id                                     as mat_patient,\n"
		        + "             d.latest_mat_visit_date,\n"
		        + "             d.latest_mat_test_results,\n"
		        + "             d.nvp_azt as nvp_azt_at_mat,\n"
		        + "             p.patient_id                                     as pnc_patient,\n"
		        + "             p.latest_pnc_visit                               as latest_pnc_visit_date,\n"
		        + "             p.latest_test_results_at_pnc                     as pnc_test_result,\n"
		        + "             p.nvp_azt as nvp_azt_at_pnc,\n"
		        + "             t.patient_id                                     as hts_patient,\n"
		        + "             t.hts_test_date                                  as latest_hts_test_date,\n"
		        + "             t.final_hts_result                               as hts_results,\n"
		        + "             h.patient_id                                     as hiv_patient\n"
		        + "      from kenyaemr_etl.etl_mch_enrollment e\n"
		        + "               left join (select a.patient_id,\n"
		        + "                                 max(a.visit_date)                                       as latest_anc_visit_date,\n"
		        + "                                 mid(max(concat(a.visit_date, a.final_test_result)), 11) as latest_anc_test_results,\n"
		        + "                          group_concat(a.baby_nvp_dispensed,a.baby_azt_dispensed) as nvp_azt\n"
		        + "                          from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                          where date(a.visit_date) <= date(:endDate)\n"
		        + "                          group by a.patient_id) a\n"
		        + "                         on e.patient_id = a.patient_id\n"
		        + "               left join (select d.patient_id,\n"
		        + "                                  max(d.visit_date)                                       as latest_mat_visit_date,\n"
		        + "                                 mid(max(concat(d.visit_date, d.final_test_result)), 11) as latest_mat_test_results,\n"
		        + "                          group_concat(d.baby_nvp_dispensed,d.baby_azt_dispensed) as nvp_azt\n"
		        + "                          from kenyaemr_etl.etl_mchs_delivery d\n"
		        + "                          where date(d.visit_date) <= date(:endDate)\n"
		        + "                          group by d.patient_id) d\n"
		        + "                         on e.patient_id = d.patient_id\n"
		        + "               left join (select p.patient_id,\n"
		        + "                                 max(p.visit_date)                                       as latest_pnc_visit,\n"
		        + "                                 mid(max(concat(p.visit_date, p.final_test_result)), 11) as latest_test_results_at_pnc,\n"
		        + "                          group_concat(p.baby_nvp_dispensed,p.baby_azt_dispensed) as nvp_azt\n"
		        + "                          from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                          where date(p.visit_date) <= date(:endDate)\n"
		        + "                          group by p.patient_id) p\n"
		        + "                         on e.patient_id = p.patient_id\n"
		        + "               left join (select t.patient_id,\n"
		        + "                                 max(t.visit_date)                                       as hts_test_date,\n"
		        + "                                 mid(max(concat(t.visit_date, t.final_test_result)), 11) as final_hts_result\n"
		        + "                          from kenyaemr_etl.etl_hts_test t\n"
		        + "                          where date(t.visit_date) <= date(:endDate)\n"
		        + "                          group by t.patient_id) t on e.patient_id = t.patient_id\n"
		        + "               left join kenyaemr_etl.etl_hiv_enrollment h on e.patient_id = h.patient_id\n"
		        + "      group by e.patient_id) c\n"
		        + "where date(latest_mch_enrollment) between date(:startDate) and date(:endDate)\n"
		        + "  and (c.hiv_patient is null or c.hts_results = 'Positive')\n"
		        + "  and c.nvp_azt_at_anc is null and c.nvp_azt_at_mat = '00' and c.nvp_azt_at_pnc is null;";
		
		SqlQueryBuilder builder = new SqlQueryBuilder();
		builder.append(qry);
		Date startDate = (Date) context.getParameterValue("startDate");
		Date endDate = (Date) context.getParameterValue("endDate");
		builder.addParameter("startDate", startDate);
		builder.addParameter("endDate", endDate);
		List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
		
		newCohort.setMemberIds(new HashSet<Integer>(ptIds));
		
		return new EvaluatedCohort(newCohort, definition, context);
	}
	
}
