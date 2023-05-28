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
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.rri.CALHIVCohortDefinition;
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.rri.TxCurrPregnantAndBfCohortDefinition;
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
 * Evaluator for TxCurrPregnantAndBfCohortDefinition
 */
@Handler(supports = { TxCurrPregnantAndBfCohortDefinition.class })
public class TxCurrPregnantAndBfCohortDefinitionEvaluator implements CohortDefinitionEvaluator {
	
	private final Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	EvaluationService evaluationService;
	
	@Override
	public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {
		
		TxCurrPregnantAndBfCohortDefinition definition = (TxCurrPregnantAndBfCohortDefinition) cohortDefinition;
		
		if (definition == null)
			return null;
		
		Cohort newCohort = new Cohort();
		
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "       select fup.visit_date,\n"
		        + "         fup.patient_id,\n"
		        + "         max(e.visit_date)                                               as enroll_date,\n"
		        + "         greatest(max(e.visit_date),ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "         greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "         greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                  ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "         d.patient_id                                                           as disc_patient,\n"
		        + "         d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "         max(d.visit_date)                                                      as date_discontinued,\n"
		        + "         de.patient_id                                                          as started_on_drugs,\n"
		        + "         max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "         timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "         de.date_started,\n"
		        + "         c.baby_feeding_method,\n"
		        + "         mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "         c.anc_client,\n"
		        + "         mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "       from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "         join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "         join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "         left join (select c.patient_id,\n"
		        + "                      max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                      m.visit_date          as disc_visit,\n"
		        + "                      m.effective_disc_date as effective_disc_date,\n"
		        + "                      m.patient_id          as disc_client,\n"
		        + "                      p.baby_feeding_method,\n"
		        + "                      a.patient_id          as anc_client\n"
		        + "                    from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                      left join (select p.patient_id,\n"
		        + "                                   max(p.visit_date)                                         as latest_visit,\n"
		        + "                                   mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                 from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                 where p.visit_date <= date(:endDate)\n"
		        + "                                 group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                      left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                 from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                 where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                 group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                      left join (select patient_id,\n"
		        + "                                   max(visit_date) as visit_date,\n"
		        + "                                   mid(\n"
		        + "                                       max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                       11)     as effective_disc_date\n"
		        + "                                 from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                 where date(visit_date) <= date(:endDate)\n"
		        + "                                       and program_name = 'MCH-Mother Services'\n"
		        + "                                 group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                    where c.visit_date <= date(:endDate)\n"
		        + "                          and c.service_type in (1622, 1623)\n"
		        + "                    group by c.patient_id\n"
		        + "                    having (disc_client is null or\n"
		        + "                            (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "           on fup.patient_id = c.patient_id\n"
		        + "         left outer join (select de.patient_id,\n"
		        + "                            min(date(de.date_started)) as date_started,\n"
		        + "                            de.program                 as program\n"
		        + "                          from kenyaemr_etl.etl_drug_event de\n"
		        + "                          group by de.patient_id) de\n"
		        + "           on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "              date(date_started) <= date(:endDate)\n"
		        + "         left outer JOIN\n"
		        + "         (select patient_id,\n"
		        + "            coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "            max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "          from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "          where date(visit_date) <= date(:endDate)\n"
		        + "                and program_name = 'HIV'\n"
		        + "          group by patient_id\n"
		        + "         ) d on d.patient_id = fup.patient_id\n"
		        + "       where fup.visit_date <= date(:endDate)\n"
		        + "       group by patient_id\n"
		        + "       having (started_on_drugs is not null and started_on_drugs <> '')\n"
		        + "              and (\n"
		        + "                (\n"
		        + "                  ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30) and\n"
		        + "                   ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                     date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                  and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                       date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                ))\n"
		        + "              and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                   anc_client is not null)\n" + "              and TI_on_art = 0) t ;";
		
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
