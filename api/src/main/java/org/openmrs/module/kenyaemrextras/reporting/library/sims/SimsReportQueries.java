package org.openmrs.module.kenyaemrextras.reporting.library.sims;

public class SimsReportQueries {
	
	public static String missedAppointmentQuery() {
		String qry = "select t.patient_id\n"
		        + "from(\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "d.patient_id as disc_patient,\n"
		        + "d.effective_disc_date as effective_disc_date,\n"
		        + "max(d.visit_date) as date_discontinued,\n"
		        + "d.discontinuation_reason,\n"
		        + "de.patient_id as started_on_drugs\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "left outer JOIN\n"
		        + "(select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date,discontinuation_reason from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "group by patient_id\n"
		        + ") d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate)\n"
		        + "group by patient_id\n"
		        + "having (\n"
		        + "(timestampdiff(DAY,date(latest_tca),date(:endDate)) between 1 and 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null)\n"
		        + "and (date(latest_vis_date) > date(date_discontinued) and date(latest_tca) > date(date_discontinued) or disc_patient is null)\n"
		        + ")\n" + ") t order by RAND() limit 10;";
		return qry;
	}
	
	public static String newlyDiagnosedQuery() {
		String qry = "select e.patient_id\n"
		        + "from kenyaemr_etl.etl_hiv_enrollment e \n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id\n"
		        + "left join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id = e.patient_id \n"
		        + "and fup.visit_date between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH) and date(:endDate)\n"
		        + "where date(e.date_confirmed_hiv_positive) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH) and date(:endDate) \n"
		        + "and timestampdiff(YEAR ,p.dob,date(:endDate)) >= 15 order by RAND() limit 10";
		return qry;
	}
	
	/**
	 * Adult and adolescent patients on ART ≥ 12 months with virologic non-suppression.
	 */
	public static String unSupressedVLQuery() {
		String qry = "select a.patient_id as patient_id\n"
		        + "                        from(select t.patient_id,vl.vl_date,vl.lab_test,vl.vl_result,vl.urgency from (\n"
		        + "                        select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "                               greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "                               greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "                               greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "                               d.patient_id as disc_patient,\n"
		        + "                               d.effective_disc_date as effective_disc_date,\n"
		        + "                               max(d.visit_date) as date_discontinued,\n"
		        + "                               de.patient_id as started_on_drugs,\n"
		        + "                               de.date_started,\n"
		        + "                               timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "                        from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                               join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                               join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                               left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and de.date_started <= date_sub(date(:endDate) , interval 12 MONTH)\n"
		        + "                               left outer JOIN\n"
		        + "                                 (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                  where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                                  group by patient_id\n"
		        + "                                 ) d on d.patient_id = fup.patient_id\n"
		        + "                        where fup.visit_date <= date(:endDate)\n"
		        + "                        group by patient_id\n"
		        + "                        having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "                            (\n"
		        + "                                ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                                  and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15\n"
		        + "                                )\n"
		        + "                            ) order by date_started desc\n"
		        + "                        ) t\n"
		        + "                          inner join (\n"
		        + "                                     select\n"
		        + "                           b.patient_id,\n"
		        + "                           max(b.visit_date) as vl_date,\n"
		        + "                           date_sub(date(:endDate) , interval 12 MONTH),\n"
		        + "                           mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n"
		        + "                           if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n"
		        + "                           mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n"
		        + "                                           from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n"
		        + "                                           from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n"
		        + "                                           group by x.patient_id,x.visit_date order by visit_date desc)b\n"
		        + "                                     group by patient_id\n"
		        + "                                     having max(visit_date) between\n"
		        + "                        date_sub(date(:endDate) , interval 12 MONTH) and date(:endDate)\n"
		        + "                                     )vl\n"
		        + "                            on t.patient_id = vl.patient_id where vl_result >= 1000)a  order by RAND() limit 10";
		return qry;
	}
	
	/**
	 * Instructions: Review 10 register entries (individual or index/partner testing logbook) or
	 * charts (whichever source has the most updated information) of HIV-positive adult and
	 * adolescent patients ≥15 years old on ART ≥12 months.
	 */
	public static String currentlyOnArtQuery() {
		String qry = "select t.patient_id from (\n"
		        + "    select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "            greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "            greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "            greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "            d.patient_id as disc_patient,\n"
		        + "            d.effective_disc_date as effective_disc_date,\n"
		        + "            max(d.visit_date) as date_discontinued,\n"
		        + "            de.patient_id as started_on_drugs,\n"
		        + "            de.date_started,\n"
		        + "            timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "    from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "            join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "            join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "            left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and de.date_started <= date_sub(date(:endDate) , interval 12 MONTH)\n"
		        + "            left outer JOIN\n"
		        + "                (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "                ) d on d.patient_id = fup.patient_id\n"
		        + "    where fup.visit_date <= date(:endDate)\n"
		        + "    group by patient_id\n"
		        + "    having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "        (\n"
		        + "            ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15\n"
		        + "            )\n" + "        )\n" + "    ) t order by RAND() limit 10";
		return qry;
		
	}
	
	/**
	 * Adults current on ART with presumed TB
	 * 
	 * @return
	 */
	public static String currentOnARTWithPresumptiveTBQuery() {
		String qry = "select t.patient_id from (\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "    greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "    greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "    greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "    d.patient_id as disc_patient,\n"
		        + "    d.effective_disc_date as effective_disc_date,\n"
		        + "    max(d.visit_date) as date_discontinued,\n"
		        + "    de.patient_id as started_on_drugs,\n"
		        + "    de.date_started,\n"
		        + "    timestampdiff(YEAR ,p.dob,date(:endDate)) as age,\n"
		        + "    mid(max(concat(fup.visit_date,fup.tb_status)),11) as tbStatus\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "    join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "    join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "    left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + "    left outer JOIN\n"
		        + "        (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "        where date(visit_date) <= date(:endDate ) and program_name='HIV'\n"
		        + "        group by patient_id\n"
		        + "        ) d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate )\n"
		        + "group by patient_id\n"
		        + "having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "(\n"
		        + "    ((timestampdiff(DAY,date(latest_tca),date(:endDate )) <= 30) and ((date(d.effective_disc_date) > date(:endDate ) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "        and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15 and tbStatus =142177 \n"
		        + "    )        ) ) t order by RAND() limit 10";
		return qry;
	}
	
	/**
	 * Pediatric patients <15 years old on ART ≥12 months
	 * 
	 * @return
	 */
	public static String pedsCurrentlyOnArtMoreThan12MonthsQuery() {
		String qry = "select t.patient_id from (\n"
		        + "    select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "            greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "            greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "            greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "            d.patient_id as disc_patient,\n"
		        + "            d.effective_disc_date as effective_disc_date,\n"
		        + "            max(d.visit_date) as date_discontinued,\n"
		        + "            de.patient_id as started_on_drugs,\n"
		        + "            de.date_started,\n"
		        + "            timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "    from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "            join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "            join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "            left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and de.date_started <= date_sub(date(:endDate) , interval 12 MONTH)\n"
		        + "            left outer JOIN\n"
		        + "                (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "                ) d on d.patient_id = fup.patient_id\n"
		        + "    where fup.visit_date <= date(:endDate)\n"
		        + "    group by patient_id\n"
		        + "    having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "        (\n"
		        + "            ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age <15\n"
		        + "            )\n" + "        ) \n" + "    ) t  order by RAND() limit 10";
		return qry;
		
	}
	
	/**
	 * Pediatric ART patients: Review tracking documentation for the last 10 pediatric ART patients
	 * who missed their most recent appointment.
	 * 
	 * @return
	 */
	public static String pedMissedAppointment() {
		String qry = "select t.patient_id from(\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "d.patient_id as disc_patient,\n"
		        + "d.effective_disc_date as effective_disc_date,\n"
		        + "max(d.visit_date) as date_discontinued,\n"
		        + "d.discontinuation_reason,\n"
		        + "de.patient_id as started_on_drugs,\n"
		        + "timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "left outer JOIN\n"
		        + "(select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date,discontinuation_reason from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "group by patient_id\n"
		        + ") d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate)\n"
		        + "group by patient_id\n"
		        + "having (\n"
		        + "(timestampdiff(DAY,date(latest_tca),date(:endDate)) between 1 and 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null)\n"
		        + "and (date(latest_vis_date) > date(date_discontinued) and date(latest_tca) > date(date_discontinued) or disc_patient is null) and age <15\n"
		        + ") ) t order by RAND() limit 10;";
		return qry;
	}
	
	public static String pedNewlyDiagnosedQuery() {
		String qry = "select e.patient_id\n"
		        + " from kenyaemr_etl.etl_hiv_enrollment e \n"
		        + " join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id\n"
		        + " left join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id = e.patient_id and fup.visit_date between date(:startDate) and date(:endDate)\n"
		        + " where date(e.date_confirmed_hiv_positive) between date(:startDate) and date(:endDate) and timestampdiff(YEAR ,p.dob,date(:endDate)) < 15 ";
		return qry;
	}
	
	/**
	 * All women screened 90 days prior OR the previous 10 entries/records (whichever is less), of
	 * women with positive cervical cancer screening test results
	 * 
	 * @return
	 */
	public static String adultsOnArtScreenedForCervicalCancerQuery() {
		String qry = "\n"
		        + "select t.patient_id from (\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "        greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "        greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "        greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "        d.patient_id as disc_patient,\n"
		        + "        d.effective_disc_date as effective_disc_date,\n"
		        + "        max(d.visit_date) as date_discontinued,\n"
		        + "        de.patient_id as started_on_drugs,\n"
		        + "        de.date_started,\n"
		        + "        timestampdiff(YEAR ,p.dob,date(:endDate)) as age,\n"
		        + "        p.gender as gender,\n"
		        + "        mid(max(concat(cs.visit_date, cs.screening_result)), 11)  as screening_result\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "        join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "        join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "        left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + "        left outer JOIN\n"
		        + "            (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "            where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "            group by patient_id\n"
		        + "            ) d on d.patient_id = fup.patient_id\n"
		        + "    join kenyaemr_etl.etl_cervical_cancer_screening cs on e.patient_id = cs.patient_id  and cs.visit_date between date_sub(date(:endDate) , interval 90 DAY) and date(:endDate)\n"
		        + "\n"
		        + "where fup.visit_date <= date(:endDate)\n"
		        + "group by patient_id\n"
		        + "having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "    (\n"
		        + "        ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "            and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15 and gender='F' and screening_result ='Positive'\n"
		        + "        )   )  ) t order by RAND() limit 10 ";
		return qry;
	}
	
	/**
	 * TX_CURR KPs with clinical encounters within the last 12 months
	 * 
	 * @return
	 */
	public static String txCurrKPsWithVisitsLast12Months() {
		String qry = "select t.patient_id\n"
		        + "        from(\n"
		        + "            select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "                   greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "                   greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "                   greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "                   d.patient_id as disc_patient,\n"
		        + "                   d.effective_disc_date as effective_disc_date,\n"
		        + "                   max(d.visit_date) as date_discontinued,\n"
		        + "                   de.patient_id as started_on_drugs\n"
		        + "            from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                   join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                   join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                   join (select v.client_id\n"
		        + "                      from kenyaemr_etl.etl_clinical_visit v\n"
		        + "                      where v.visit_date between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "                                and date(:endDate)) cv on fup.patient_id = cv.client_id\n"
		        + "                   left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "                   left outer JOIN\n"
		        + "                     (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                      where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                      group by patient_id\n"
		        + "                     ) d on d.patient_id = fup.patient_id\n"
		        + "            where fup.visit_date <= date(:endDate)\n"
		        + "            group by patient_id\n"
		        + "            having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "                (\n"
		        + "                    ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                      and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                    ))) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * TX_CURR KPs with clinical encounters within the last 3 months
	 * 
	 * @return
	 */
	public static String txCurrKPsWithVisitsLast3Months() {
		String qry = "select t.patient_id\n"
		        + "from(\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "d.patient_id as disc_patient,\n"
		        + "d.effective_disc_date as effective_disc_date,\n"
		        + "max(d.visit_date) as date_discontinued,\n"
		        + "de.patient_id as started_on_drugs\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "join (select v.client_id\n"
		        + "from kenyaemr_etl.etl_clinical_visit v\n"
		        + "where v.visit_date between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH)\n"
		        + "       and date(:endDate)) cv on fup.patient_id = cv.client_id\n"
		        + "left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "left outer JOIN\n"
		        + "(select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "group by patient_id\n"
		        + ") d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate)\n"
		        + "group by patient_id\n"
		        + "having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "(\n"
		        + "((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "))) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * TX_CURR KPs aged at least 15 years newly started on ART
	 * 
	 * @return
	 */
	public static String txCurrKPsAgedAtleast15NewOnART() {
		String qry = "select t.patient_id\n"
		        + "        from(\n"
		        + "        select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "        greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "        greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "        greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "        d.patient_id as disc_patient,\n"
		        + "        d.effective_disc_date as effective_disc_date,\n"
		        + "        max(d.visit_date) as date_discontinued,\n"
		        + "        de.patient_id as started_on_drugs,\n"
		        + "               max(if(e.date_started_art_at_transferring_facility is not null and e.facility_transferred_from is not null, 1, 0)) as TI_on_art,\n"
		        + "        timestampdiff(YEAR, p.DOB, date(:endDate)) as age,\n"
		        + "               de.date_started\n"
		        + "        from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "        join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "        join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "        join (select c.client_id\n"
		        + "        from kenyaemr_etl.etl_contact c\n"
		        + "        where c.visit_date <= date(:endDate)) c on fup.patient_id = c.client_id\n"
		        + "        left outer join (select de.patient_id,min(date(de.date_started)) as date_started, de.program as program from kenyaemr_etl.etl_drug_event de group by de.patient_id) de\n"
		        + "          on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "        left outer JOIN\n"
		        + "        (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "        where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "        group by patient_id\n"
		        + "        ) d on d.patient_id = fup.patient_id\n"
		        + "        where fup.visit_date <= date(:endDate)\n"
		        + "        group by patient_id\n"
		        + "        having (started_on_drugs is not null and started_on_drugs <> '' and timestampdiff(MONTH, date_started, date(:endDate)) <= 3) and (\n"
		        + "        (\n"
		        + "        ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "        and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "        ) and age >= 15) and TI_on_art = 0) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * Missed Appointment KPs
	 * 
	 * @return
	 */
	public static String missedAppKPs() {
		String query = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                                as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))                    as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00'))           as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                                as latest_tca,\n"
		        + "                d.patient_id                                                                     as disc_patient,\n"
		        + "                d.effective_disc_date                                                            as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                                as date_discontinued,\n"
		        + "                d.discontinuation_reason,\n"
		        + "                de.patient_id                                                                    as started_on_drugs,\n"
		        + "                c.latest_kp_enrolment\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  join(\n"
		        + "             select c.client_id, max(c.visit_date) as latest_kp_enrolment\n"
		        + "             from kenyaemr_etl.etl_contact c\n"
		        + "             where date(c.visit_date) <= date(:endDate)\n"
		        + "             group by c.client_id\n"
		        + "         ) c on fup.patient_id = c.client_id\n"
		        + "                  left outer join kenyaemr_etl.etl_drug_event de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date,\n"
		        + "                      discontinuation_reason\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (\n"
		        + "                        (timestampdiff(DAY, date(latest_tca), date(:endDate)) between 1 and 30) and\n"
		        + "                        ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                          date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null)\n"
		        + "                        and (date(latest_vis_date) > date(date_discontinued) and\n"
		        + "                             date(latest_tca) > date(date_discontinued) or disc_patient is null)\n"
		        + "                    )) t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * Pediatric ART patients: Review the last 5 entries in the line list/register of HIV-positive
	 * pediatric patients <15 years with presumptive TB recorded in line list/register. S_02_29
	 * 
	 * @return
	 */
	public static String pedscurrentOnARTWithPresumptiveTBQuery() {
		String qry = "select t.patient_id from (\n"
		        + "  select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "      greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "      greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "      greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "      d.patient_id as disc_patient,\n"
		        + "      d.effective_disc_date as effective_disc_date,\n"
		        + "      max(d.visit_date) as date_discontinued,\n"
		        + "      de.patient_id as started_on_drugs,\n"
		        + "      de.date_started,\n"
		        + "      timestampdiff(YEAR ,p.dob,date(:endDate)) as age,\n"
		        + "      mid(max(concat(fup.visit_date,fup.tb_status)),11) as tbStatus\n"
		        + "  from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "      join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "      join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "      left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + "      left outer JOIN\n"
		        + "          (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "          where date(visit_date) <= date(:endDate ) and program_name='HIV'\n"
		        + "          group by patient_id\n"
		        + "          ) d on d.patient_id = fup.patient_id\n"
		        + "  where fup.visit_date <= date(:endDate )\n"
		        + "  group by patient_id\n"
		        + "  having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "  (\n"
		        + "      ((timestampdiff(DAY,date(latest_tca),date(:endDate )) <= 30) and ((date(d.effective_disc_date) > date(:endDate ) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "          and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age <15 and tbStatus =142177 \n"
		        + "      )        ) ) t order by RAND() limit 5";
		return qry;
	}
	
	/**
	 * Cohort definition :S_03_14-16_Q3 In KP program In HIV program In art >= 12 months Age 15
	 * years and above
	 * 
	 * @return
	 */
	public static String txCurrKpMoreThan12MonthsOnArtQuery() {
		String qry = "select t.patient_id\n"
		        + "    from(\n"
		        + "        select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "               greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "               greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "               greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "               d.patient_id as disc_patient,\n"
		        + "               d.effective_disc_date as effective_disc_date,\n"
		        + "               max(d.visit_date) as date_discontinued,\n"
		        + "               de.patient_id as started_on_drugs,\n"
		        + "     timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "        from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "               join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "               join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "join(\n"
		        + "select c.client_id from kenyaemr_etl.etl_contact c\n"
		        + "left join (select p.client_id from kenyaemr_etl.etl_peer_calendar p where p.voided = 0 group by p.client_id having max(p.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cp on c.client_id=cp.client_id\n"
		        + "left join (select v.client_id from kenyaemr_etl.etl_clinical_visit v where v.voided = 0 group by v.client_id having max(v.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cv on c.client_id=cv.client_id\n"
		        + "left join (select d.patient_id, max(d.visit_date) latest_visit from kenyaemr_etl.etl_patient_program_discontinuation d where d.program_name='KP' group by d.patient_id) d on c.client_id = d.patient_id\n"
		        + "where (d.patient_id is null or d.latest_visit > date(:endDate)) and c.voided = 0  and (cp.client_id is not null or cv.client_id is not null) group by c.client_id\n"
		        + ") kp on kp.client_id = fup.patient_id\n"
		        + "           join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + " and de.date_started <= date(:endDate) and timestampdiff(MONTH,date(de.date_started), date(:endDate)) >=12\n"
		        + "           left outer JOIN\n"
		        + "                 (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                  where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                  group by patient_id\n"
		        + "                 ) d on d.patient_id = fup.patient_id\n"
		        + "        where fup.visit_date <= date(:endDate)\n"
		        + "        group by patient_id\n"
		        + "        having\n"
		        + "            (\n"
		        + "                ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                  and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15\n"
		        + "                )order by de.date_started desc) t limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :S_03_12_Q3 In KP program In HIV program In art >= 12 months Age 15 years
	 * and above Who had ≥1000 copies/mL in the most recent viral load
	 * 
	 * @return
	 */
	public static String KpUnSupressedVLQuery() {
		String qry = "select t.patient_id\n"
		        + "from(\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "d.patient_id as disc_patient,\n"
		        + "d.effective_disc_date as effective_disc_date,\n"
		        + "max(d.visit_date) as date_discontinued,\n"
		        + "de.patient_id as started_on_drugs,\n"
		        + "timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "join(\n"
		        + "select c.client_id from kenyaemr_etl.etl_contact c\n"
		        + "left join (select p.client_id from kenyaemr_etl.etl_peer_calendar p where p.voided = 0 group by p.client_id having max(p.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cp on c.client_id=cp.client_id\n"
		        + "left join (select v.client_id from kenyaemr_etl.etl_clinical_visit v where v.voided = 0 group by v.client_id having max(v.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cv on c.client_id=cv.client_id\n"
		        + "left join (select d.patient_id, max(d.visit_date) latest_visit from kenyaemr_etl.etl_patient_program_discontinuation d where d.program_name='KP' group by d.patient_id) d on c.client_id = d.patient_id\n"
		        + "where (d.patient_id is null or d.latest_visit > date(:endDate)) and c.voided = 0  and (cp.client_id is not null or cv.client_id is not null) group by c.client_id\n"
		        + ") kp on kp.client_id = fup.patient_id\n"
		        + "join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + " and de.date_started <= date(:endDate) and timestampdiff(MONTH,date(de.date_started), date(:endDate)) >=12\n"
		        + "join (\n"
		        + " select b.patient_id,max(b.visit_date) as vl_date, date_sub(date(:endDate) , interval 12 MONTH),mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n"
		        + " if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, 'LDL','')) as vl_result,\n"
		        + " mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n"
		        + " from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n"
		        + " from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n"
		        + " group by x.patient_id,x.visit_date order by visit_date desc)b group by patient_id\n"
		        + " having max(visit_date) between date_sub(date(:endDate) , interval 12 MONTH) and date(:endDate)\n"
		        + " )vl  on fup.patient_id = vl.patient_id\n"
		        + "left outer JOIN\n"
		        + "(select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "group by patient_id\n"
		        + ") d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate) and vl.vl_result >= 1000\n"
		        + "group by patient_id\n"
		        + "having\n"
		        + "(\n"
		        + "((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15\n"
		        + ")order by de.date_started desc) t limit 10;";
		return qry;
	}
	
	/**
	 * New tested HIV +ve KPs rapid ART initiation who had clinical visits last 3 months
	 * 
	 * @return
	 */
	public static String rapidARTInitiationKPs() {
		String query = "select t.patient_id\n"
		        + "from(\n"
		        + "select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "d.patient_id as disc_patient,\n"
		        + "d.effective_disc_date as effective_disc_date,\n"
		        + "max(d.visit_date) as date_discontinued,\n"
		        + "de.patient_id as started_on_drugs,\n"
		        + "               timestampdiff(YEAR, date(p.DOB), date(:endDate))                  as age\n"
		        + "from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "join (select v.client_id,\n"
		        + "                     max(c.visit_date)     as latest_enrollment,\n"
		        + "                     p.patient_id          as disc_client,\n"
		        + "                     p.visit_date          as disc_visit,\n"
		        + "                     p.effective_disc_date as effective_disc_date\n"
		        + "              from kenyaemr_etl.etl_clinical_visit v\n"
		        + "                       inner join kenyaemr_etl.etl_contact c on v.client_id = c.client_id\n"
		        + "                       left join (select patient_id,\n"
		        + "                                         max(visit_date)                                                              as visit_date,\n"
		        + "                                         mid(max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                             11)                                                                      as effective_disc_date\n"
		        + "                                  from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                  where date(visit_date) <= date(:endDate)\n"
		        + "                                    and program_name = 'KP'\n"
		        + "                                  group by patient_id) p on v.client_id = p.patient_id\n"
		        + "              where v.visit_date between date_sub(date(:endDate), INTERVAL 90 DAY) and date(:endDate)\n"
		        + "                and c.visit_date <= date(:endDate) and c.key_population_type <> '' and c.key_population_type is not null\n"
		        + "              group by v.client_id\n"
		        + "              having (disc_client is null or ((latest_enrollment > coalesce(effective_disc_date, disc_visit) and\n"
		        + "                                              max(v.visit_date) >= coalesce(effective_disc_date, disc_visit))\n"
		        + "                  or effective_disc_date > date(:endDate)))) cv on fup.patient_id = cv.client_id\n"
		        + "left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "left outer JOIN\n"
		        + "(select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "group by patient_id\n"
		        + ") d on d.patient_id = fup.patient_id\n"
		        + "where fup.visit_date <= date(:endDate) and date(e.date_confirmed_hiv_positive) between date(:startDate) and date(:endDate)\n"
		        + "group by patient_id\n"
		        + "having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "(\n"
		        + "((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >= 15\n"
		        + "))) t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * TX_CURR KPs on TX for at least 12 months
	 * 
	 * @return
	 */
	public static String txCurrKPsOnTXAtleast12Months() {
		String query = "select t.patient_id\n"
		        + "       from(\n"
		        + "       select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "       greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "       greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "       greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "       d.patient_id as disc_patient,\n"
		        + "       d.effective_disc_date as effective_disc_date,\n"
		        + "       max(d.visit_date) as date_discontinued,\n"
		        + "       de.patient_id as started_on_drugs,\n"
		        + "              max(if(e.date_started_art_at_transferring_facility is not null and e.facility_transferred_from is not null, 1, 0)) as TI_on_art,\n"
		        + "       timestampdiff(YEAR, p.DOB, date(:endDate)) as age,\n"
		        + "              de.date_started\n"
		        + "       from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "       join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "       join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "       join (select c.client_id,\n"
		        + "                                max(c.visit_date)     as latest_enrollment,\n"
		        + "                                p.patient_id          as disc_client,\n"
		        + "                                p.visit_date          as disc_visit,\n"
		        + "                                p.effective_disc_date as effective_disc_date\n"
		        + "                         from kenyaemr_etl.etl_contact c\n"
		        + "                                  left join (select patient_id,\n"
		        + "                                                    max(visit_date)                                                              as visit_date,\n"
		        + "                                                    mid(max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                        11)                                                                      as effective_disc_date\n"
		        + "                                             from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                             where date(visit_date) <= date(:endDate)\n"
		        + "                                               and program_name = 'KP'\n"
		        + "                                             group by patient_id) p on c.client_id = p.patient_id\n"
		        + "                         where c.visit_date <= date(:endDate) and c.key_population_type <> '' and c.key_population_type is not null\n"
		        + "                         group by c.client_id\n"
		        + "                         having (disc_client is null or (latest_enrollment > coalesce(effective_disc_date, disc_visit)\n"
		        + "                             or effective_disc_date > date(:endDate)))) c on fup.patient_id = c.client_id\n"
		        + "       left outer join (select de.patient_id,min(date(de.date_started)) as date_started, de.program as program from kenyaemr_etl.etl_drug_event de group by de.patient_id) de\n"
		        + "         on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "       left outer JOIN\n"
		        + "       (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "       where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "       group by patient_id\n"
		        + "       ) d on d.patient_id = fup.patient_id\n"
		        + "       where fup.visit_date <= date(:endDate)\n"
		        + "       group by patient_id\n"
		        + "       having (started_on_drugs is not null and started_on_drugs <> '' and timestampdiff(MONTH, date_started, date(:endDate)) >= 12) and (\n"
		        + "       (\n"
		        + "       ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "       and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "       ) and age >= 15) and TI_on_art = 0) t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * TX_CURR KPs with presumed TB
	 * 
	 * @return
	 */
	public static String txCurrKPsWithPresumtiveTB() {
		String query = "select t.patient_id\n"
		        + "from(\n"
		        + "        select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "               greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "               greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "               greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "               d.patient_id as disc_patient,\n"
		        + "               d.effective_disc_date as effective_disc_date,\n"
		        + "               max(d.visit_date) as date_discontinued,\n"
		        + "               de.patient_id as started_on_drugs,\n"
		        + "               max(if(e.date_started_art_at_transferring_facility is not null and e.facility_transferred_from is not null, 1, 0)) as TI_on_art,\n"
		        + "               timestampdiff(YEAR, p.DOB, date(:endDate)) as age,\n"
		        + "               mid(max(concat(fup.visit_date,fup.tb_status)),11) as tb_case\n"
		        + "        from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                 join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                 join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                 join (select c.client_id,\n"
		        + "                              max(c.visit_date)     as latest_enrollment,\n"
		        + "                              p.patient_id          as disc_client,\n"
		        + "                              p.visit_date          as disc_visit,\n"
		        + "                              p.effective_disc_date as effective_disc_date\n"
		        + "                       from kenyaemr_etl.etl_contact c\n"
		        + "                                left join (select patient_id,\n"
		        + "                                                  max(visit_date)                                                              as visit_date,\n"
		        + "                                                  mid(max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                      11)                                                                      as effective_disc_date\n"
		        + "                                           from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                           where date(visit_date) <= date(:endDate)\n"
		        + "                                             and program_name = 'KP'\n"
		        + "                                           group by patient_id) p on c.client_id = p.patient_id\n"
		        + "                       where c.visit_date <= date(:endDate) and c.key_population_type <> '' and c.key_population_type is not null\n"
		        + "                       group by c.client_id\n"
		        + "                       having (disc_client is null or (latest_enrollment > coalesce(effective_disc_date, disc_visit)\n"
		        + "                           or effective_disc_date > date(:endDate)))) c on fup.patient_id = c.client_id\n"
		        + "                 left outer join (select de.patient_id,min(date(de.date_started)) as date_started, de.program as program from kenyaemr_etl.etl_drug_event de group by de.patient_id) de\n"
		        + "                                 on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "                 left outer JOIN\n"
		        + "             (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "              where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "              group by patient_id\n"
		        + "             ) d on d.patient_id = fup.patient_id\n"
		        + "        where fup.visit_date <= date(:endDate)\n"
		        + "        group by patient_id\n"
		        + "        having (started_on_drugs is not null and started_on_drugs <> '' ) and (\n"
		        + "                (\n"
		        + "                        ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                        and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                    ) and age >= 15) and TI_on_art = 0 and tb_case =142177)t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * Pediatric patients: In the selected register, review the last 10 patient entries to check for
	 * documented HIV status (e.g., positive, negative, declined) Clarification was made that these
	 * are peds listed by the index client as contacts(S_02_25)
	 * 
	 * @return
	 */
	public static String pedListedAsContacts() {
		String qry = "select c.patient_id from kenyaemr_etl.etl_patient_contact c\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=c.patient_id\n"
		        + "where c.relationship_type = 1528 and  (timestampdiff(YEAR ,date(p.dob),date(:endDate)) < 15)\n"
		        + "and c.patient_id is not null and c.patient_id != 0 and c.voided = 0\n" + "order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * TX_CURR KPs screened positive for CACX
	 * 
	 * @return
	 */
	public static String txCurrKPsCacxPositive() {
		String query = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                                as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))                    as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00'))           as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                                as latest_tca,\n"
		        + "                d.patient_id                                                                     as disc_patient,\n"
		        + "                d.effective_disc_date                                                            as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                                as date_discontinued,\n"
		        + "                de.patient_id                                                                    as started_on_drugs,\n"
		        + "                timestampdiff(YEAR, date(p.DOB), date(:endDate))                                 as age,\n"
		        + "                s.patient_id                                                                     as gen_scrn,\n"
		        + "                cv.client_id                                                                     as kp_scrn\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  join (select c.client_id,\n"
		        + "                               max(c.visit_date)     as latest_enrollment,\n"
		        + "                               p.patient_id          as disc_client,\n"
		        + "                               p.visit_date          as disc_visit,\n"
		        + "                               p.effective_disc_date as effective_disc_date\n"
		        + "                        from kenyaemr_etl.etl_contact c\n"
		        + "                                 left join (select patient_id,\n"
		        + "                                                   max(visit_date)                                                              as visit_date,\n"
		        + "                                                   mid(max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                       11)                                                                      as effective_disc_date\n"
		        + "                                            from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                            where date(visit_date) <= date(:endDate)\n"
		        + "                                              and program_name = 'KP'\n"
		        + "                                            group by patient_id) p on c.client_id = p.patient_id\n"
		        + "                        where c.visit_date <= date(:endDate) and c.key_population_type <> '' and c.key_population_type is not null\n"
		        + "                        group by c.client_id\n"
		        + "                        having (disc_client is null or (latest_enrollment > coalesce(effective_disc_date, disc_visit)\n"
		        + "                            or effective_disc_date > date(:endDate)))) c on fup.patient_id = c.client_id\n"
		        + "                  left outer join kenyaemr_etl.etl_drug_event de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left join (select v.client_id, v.cerv_cancer_screened, v.cerv_cancer_results\n"
		        + "                             from kenyaemr_etl.etl_clinical_visit v\n"
		        + "                             where v.visit_date between date_sub(date(:endDate), INTERVAL 90 DAY)\n"
		        + "                                 and date(:endDate)\n"
		        + "                               and v.cerv_cancer_screened = 'Yes'\n"
		        + "                               and v.cerv_cancer_results = 'Positive') cv on e.patient_id = cv.client_id\n"
		        + "                  left join (select s.patient_id, s.screening_result\n"
		        + "                             from kenyaemr_etl.etl_cervical_cancer_screening s\n"
		        + "                             where s.visit_date between date_sub(date(:endDate), INTERVAL 90 DAY)\n"
		        + "                                 and date(:endDate)\n"
		        + "                               and s.screening_result = 'Positive') s on e.patient_id = s.patient_id\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "           and date(e.visit_date) between date(:startDate) and date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '')\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and\n"
		        + "                     (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or\n"
		        + "                      disc_patient is null) and (kp_scrn is not null or gen_scrn is not null)\n"
		        + "                 ))) t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * TX_NEW pregnant and breastfeeding retest documentation
	 * 
	 * @return
	 */
	public static String txNewPregnantBreastFeedingRetestDocumentation() {
		String query = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                      as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "                d.patient_id                                                           as disc_patient,\n"
		        + "                d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                      as date_discontinued,\n"
		        + "                de.patient_id                                                          as started_on_drugs,\n"
		        + "                max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                       e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "                timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "                de.date_started,\n"
		        + "                c.baby_feeding_method,\n"
		        + "                mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "                c.anc_client,\n"
		        + "                mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  left join (select c.patient_id,\n"
		        + "                               max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                               m.visit_date          as disc_visit,\n"
		        + "                               m.effective_disc_date as effective_disc_date,\n"
		        + "                               m.patient_id          as disc_client,\n"
		        + "                               p.baby_feeding_method,\n"
		        + "                               a.patient_id          as anc_client\n"
		        + "                        from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                 left join (select p.patient_id,\n"
		        + "                                                   max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                   mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                            from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                            where p.visit_date <= date(:endDate)\n"
		        + "                                            group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                 left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                            from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                            where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                            group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                 left join (select patient_id,\n"
		        + "                                                   max(visit_date) as visit_date,\n"
		        + "                                                   mid(\n"
		        + "                                                           max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                           11)     as effective_disc_date\n"
		        + "                                            from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                            where date(visit_date) <= date(:endDate)\n"
		        + "                                              and program_name = 'MCH Mother'\n"
		        + "                                            group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                        where c.visit_date <= date(:endDate)\n"
		        + "                          and c.service_type in (1622, 1623)\n"
		        + "                        group by c.patient_id\n"
		        + "                        having (disc_client is null or\n"
		        + "                                (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                       on fup.patient_id = c.patient_id\n"
		        + "                  left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                   from kenyaemr_etl.etl_drug_event de\n"
		        + "                                   group by de.patient_id) de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '' and\n"
		        + "                 timestampdiff(MONTH, date_started, date(:endDate)) <= 3)\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                          date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                 ))\n"
		        + "            and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                 anc_client is not null)\n"
		        + "            and TI_on_art = 0) t order by RAND() limit 10;";
		return query;
	}
	
	/**
	 * TX_CURR pregnant and breastfeeding missed their most recent appointment
	 * 
	 * @return
	 */
	public static String txCurrPregnantBreastFeedingMissedApp() {
		String query = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                      as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "                d.patient_id                                                           as disc_patient,\n"
		        + "                d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                      as date_discontinued,\n"
		        + "                de.patient_id                                                          as started_on_drugs,\n"
		        + "                max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                       e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "                timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "                de.date_started,\n"
		        + "                c.baby_feeding_method,\n"
		        + "                mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "                c.anc_client,\n"
		        + "                mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  left join (select c.patient_id,\n"
		        + "                               max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                               m.visit_date          as disc_visit,\n"
		        + "                               m.effective_disc_date as effective_disc_date,\n"
		        + "                               m.patient_id          as disc_client,\n"
		        + "                               p.baby_feeding_method,\n"
		        + "                               a.patient_id          as anc_client\n"
		        + "                        from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                 left join (select p.patient_id,\n"
		        + "                                                   max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                   mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                            from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                            where p.visit_date <= date(:endDate)\n"
		        + "                                            group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                 left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                            from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                            where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                            group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                 left join (select patient_id,\n"
		        + "                                                   max(visit_date) as visit_date,\n"
		        + "                                                   mid(\n"
		        + "                                                           max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                           11)     as effective_disc_date\n"
		        + "                                            from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                            where date(visit_date) <= date(:endDate)\n"
		        + "                                              and program_name = 'MCH Mother'\n"
		        + "                                            group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                        where c.visit_date <= date(:endDate)\n"
		        + "                          and c.service_type in (1622, 1623)\n"
		        + "                        group by c.patient_id\n"
		        + "                        having (disc_client is null or\n"
		        + "                                (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                       on fup.patient_id = c.patient_id\n"
		        + "                  left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                   from kenyaemr_etl.etl_drug_event de\n"
		        + "                                   group by de.patient_id) de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '')\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) between 1 and 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) between 1 and 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                          date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                 ))\n"
		        + "            and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                 anc_client is not null)\n"
		        + "            and TI_on_art = 0) t order by t.visit_date desc limit 10;";
		return query;
	}
	
	/**
	 * Instructions: Pediatric ART patients: Review 10 records (e.g., charts, high viral load
	 * register, EMR entries) of 5 pediatric (<10 years old) and 5 adolescent (10-19 years old)
	 * patients on ART ≥12 months with virologic non-suppression.
	 * 
	 * @return
	 */
	public static String pedUnsupressedVLQuery() {
		String qry = "\n"
		        + "(\n"
		        + "select a.patient_id as patient_id\n"
		        + "from(\n"
		        + "select t.patient_id,vl.vl_date,vl.lab_test,vl.vl_result,vl.urgency,age from (\n"
		        + "    select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "    greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "    greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "    greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "    d.patient_id as disc_patient,\n"
		        + "    d.effective_disc_date as effective_disc_date,\n"
		        + "    max(d.visit_date) as date_discontinued,\n"
		        + "    de.patient_id as started_on_drugs,\n"
		        + "    de.date_started,\n"
		        + "    timestampdiff(YEAR ,p.dob,date(\"2022-10-30\")) as age\n"
		        + "    from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "    join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "    join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "    left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and de.date_started <= date_sub(date(\"2022-10-30\") , interval 12 MONTH)\n"
		        + "    left outer JOIN\n"
		        + "    (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "    where date(visit_date) <= date(\"2022-10-30\") and program_name='HIV'\n"
		        + "    group by patient_id\n"
		        + "    ) d on d.patient_id = fup.patient_id\n"
		        + "    where fup.visit_date <= date(\"2022-10-30\")\n"
		        + "    group by patient_id\n"
		        + "    having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "    (\n"
		        + "    ((timestampdiff(DAY,date(latest_tca),date(\"2022-10-30\")) <= 30) and ((date(d.effective_disc_date) > date(\"2022-10-30\") or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "    and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=10 and age <= 19\n"
		        + "    )\n"
		        + "    )\n"
		        + ") t\n"
		        + "inner join (\n"
		        + "    select\n"
		        + "    b.patient_id,\n"
		        + "    max(b.visit_date) as vl_date,\n"
		        + "    date_sub(date(\"2022-10-30\") , interval 12 MONTH),\n"
		        + "    mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n"
		        + "    if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), \n"
		        + "    if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n"
		        + "    mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n"
		        + "    from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n"
		        + "    from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n"
		        + "    group by x.patient_id,x.visit_date order by visit_date desc)b\n"
		        + "    group by patient_id\n"
		        + "    having max(visit_date) between\n"
		        + "    date_sub(date(\"2022-10-30\") , interval 12 MONTH) and date(\"2022-10-30\")\n"
		        + "\n"
		        + ")vl\n"
		        + "on t.patient_id = vl.patient_id where vl_result >= 1000)a order by RAND() limit 5\n"
		        + ")\n"
		        + "union all\n"
		        + "(\n"
		        + "select a.patient_id as patient_id\n"
		        + "from(\n"
		        + "select t.patient_id,vl.vl_date,vl.lab_test,vl.vl_result,vl.urgency,age from (\n"
		        + "    select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "    greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "    greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "    greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "    d.patient_id as disc_patient,\n"
		        + "    d.effective_disc_date as effective_disc_date,\n"
		        + "    max(d.visit_date) as date_discontinued,\n"
		        + "    de.patient_id as started_on_drugs,\n"
		        + "    de.date_started,\n"
		        + "    timestampdiff(YEAR ,p.dob,date(\"2022-10-30\")) as age\n"
		        + "    from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "    join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "    join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "    left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and de.date_started <= date_sub(date(\"2022-10-30\") , interval 12 MONTH)\n"
		        + "    left outer JOIN\n"
		        + "    (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "    where date(visit_date) <= date(\"2022-10-30\") and program_name='HIV'\n"
		        + "    group by patient_id\n"
		        + "    ) d on d.patient_id = fup.patient_id\n"
		        + "    where fup.visit_date <= date(\"2022-10-30\")\n"
		        + "    group by patient_id\n"
		        + "    having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "    (\n"
		        + "    ((timestampdiff(DAY,date(latest_tca),date(\"2022-10-30\")) <= 30) and ((date(d.effective_disc_date) > date(\"2022-10-30\") or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "    and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age <10 \n"
		        + "    )\n"
		        + "    ) order by date_started desc\n"
		        + ") t\n"
		        + "inner join (\n"
		        + "    select\n"
		        + "    b.patient_id,\n"
		        + "    max(b.visit_date) as vl_date,\n"
		        + "    date_sub(date(\"2022-10-30\") , interval 12 MONTH),\n"
		        + "    mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n"
		        + "    if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), \n"
		        + "    if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n"
		        + "    mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n"
		        + "    from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n"
		        + "    from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n"
		        + "    group by x.patient_id,x.visit_date order by visit_date desc)b\n" + "    group by patient_id\n"
		        + "    having max(visit_date) between\n"
		        + "    date_sub(date(\"2022-10-30\") , interval 12 MONTH) and date(\"2022-10-30\")\n" + "\n" + ")vl\n"
		        + "on t.patient_id = vl.patient_id where vl_result >= 1000)a order by RAND() limit 5\n" + ")\n" + "\n";
		return qry;
	}
	
	/**
	 * Cohort definition :S_04_08_Q2 Pregnant or breastfeeding In art >= 12 months Age Tx Curr
	 * 
	 * @return
	 */
	public static String txCurrPregnantAndBreastfeedingOnART12MonthsQuery() {
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "          select fup.visit_date,\n"
		        + "             fup.patient_id,\n"
		        + "             max(e.visit_date)                                                      as enroll_date,\n"
		        + "             greatest(max(e.visit_date),\n"
		        + "                           ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "             greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "             greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                           ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "             d.patient_id                                                           as disc_patient,\n"
		        + "             d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "             max(d.visit_date)                                                      as date_discontinued,\n"
		        + "             de.patient_id                                                          as started_on_drugs,\n"
		        + "             max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                        e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "             timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "             de.date_started,\n"
		        + "             c.baby_feeding_method,\n"
		        + "             mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "             c.anc_client,\n"
		        + "             mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "          from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "             join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "             join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "             left join (select c.patient_id,\n"
		        + "                         max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                         m.visit_date          as disc_visit,\n"
		        + "                         m.effective_disc_date as effective_disc_date,\n"
		        + "                         m.patient_id          as disc_client,\n"
		        + "                         p.baby_feeding_method,\n"
		        + "                         a.patient_id          as anc_client\n"
		        + "                      from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                         left join (select p.patient_id,\n"
		        + "                                             max(p.visit_date)                                         as latest_visit,\n"
		        + "                                             mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                          from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                          where p.visit_date <= date(:endDate)\n"
		        + "                                          group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                         left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                          from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                          where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                          group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                         left join (select patient_id,\n"
		        + "                                             max(visit_date) as visit_date,\n"
		        + "                                             mid(\n"
		        + "                                                   max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                   11)     as effective_disc_date\n"
		        + "                                          from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                          where date(visit_date) <= date(:endDate)\n"
		        + "                                                   and program_name = 'MCH Mother'\n"
		        + "                                          group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                      where c.visit_date <= date(:endDate)\n"
		        + "                               and c.service_type in (1622, 1623)\n"
		        + "                      group by c.patient_id\n"
		        + "                      having (disc_client is null or\n"
		        + "                                  (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                on fup.patient_id = c.patient_id\n"
		        + "             left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                       from kenyaemr_etl.etl_drug_event de\n"
		        + "                                       group by de.patient_id) de\n"
		        + "                on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                     date(date_started) <= date(:endDate)\n"
		        + "             left outer JOIN\n"
		        + "             (select patient_id,\n"
		        + "                  coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                  max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                        and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "             ) d on d.patient_id = fup.patient_id\n"
		        + "          where fup.visit_date <= date(:endDate)\n"
		        + "          group by patient_id\n"
		        + "          having (started_on_drugs is not null and started_on_drugs <> ''and\n"
		        + "                      timestampdiff(MONTH, date_started, date(:endDate)) >= 12)\n"
		        + "                     and (\n"
		        + "                        (\n"
		        + "                           ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                              timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30) and\n"
		        + "                            ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                               date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                           and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                                  date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                        ))\n"
		        + "                     and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                            anc_client is not null)\n"
		        + "                     and TI_on_art = 0 order by RAND()) t limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :S_04_08_Q2 Pregnant or breastfeeding On art Tx Curr
	 * 
	 * @return
	 */
	public static String txCurrPregnantAndBreastfeedingQuery() {
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "          select fup.visit_date,\n"
		        + "             fup.patient_id,\n"
		        + "             max(e.visit_date)                                                      as enroll_date,\n"
		        + "             greatest(max(e.visit_date),\n"
		        + "                           ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "             greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "             greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                           ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "             d.patient_id                                                           as disc_patient,\n"
		        + "             d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "             max(d.visit_date)                                                      as date_discontinued,\n"
		        + "             de.patient_id                                                          as started_on_drugs,\n"
		        + "             max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                        e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "             timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "             de.date_started,\n"
		        + "             c.baby_feeding_method,\n"
		        + "             mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "             c.anc_client,\n"
		        + "             mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "          from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "             join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "             join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "             left join (select c.patient_id,\n"
		        + "                                 max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                                 m.visit_date          as disc_visit,\n"
		        + "                                 m.effective_disc_date as effective_disc_date,\n"
		        + "                                 m.patient_id          as disc_client,\n"
		        + "                                 p.baby_feeding_method,\n"
		        + "                                 a.patient_id          as anc_client\n"
		        + "                              from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                 left join (select p.patient_id,\n"
		        + "                                                    max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                    mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                                 from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                                 where p.visit_date <= date(:endDate)\n"
		        + "                                                 group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                 left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                                 from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                                 where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                                 group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                 left join (select patient_id,\n"
		        + "                                                    max(visit_date) as visit_date,\n"
		        + "                                                    mid(\n"
		        + "                                                          max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                          11)     as effective_disc_date\n"
		        + "                                                 from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                                 where date(visit_date) <= date(:endDate)\n"
		        + "                                                          and program_name = 'MCH Mother'\n"
		        + "                                                 group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                              where c.visit_date <= date(:endDate)\n"
		        + "                                       and c.service_type in (1622, 1623)\n"
		        + "                              group by c.patient_id\n"
		        + "                              having (disc_client is null or\n"
		        + "                                          (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                on fup.patient_id = c.patient_id\n"
		        + "             left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                       from kenyaemr_etl.etl_drug_event de\n"
		        + "                                       group by de.patient_id) de\n"
		        + "                on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                     date(date_started) <= date(:endDate)\n"
		        + "             left outer JOIN\n"
		        + "             (select patient_id,\n"
		        + "                  coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                  max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                        and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "             ) d on d.patient_id = fup.patient_id\n"
		        + "          where fup.visit_date <= date(:endDate)\n"
		        + "          group by patient_id\n"
		        + "          having (started_on_drugs is not null and started_on_drugs <> '')\n"
		        + "                     and (\n"
		        + "                        (\n"
		        + "                           ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                              timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                            ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                               date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                           and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                                  date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                        ))\n"
		        + "                     and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                            anc_client is not null)\n"
		        + "                     and TI_on_art = 0 order by RAND()) t limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :S_04_12_Q2 Pregnant or breastfeeding In art Tx Curr
	 * 
	 * @return
	 */
	public static String txCurrPregnantAndBFPresumedTbQuery() {
		String qry = "select t.patient_id\n"
		        + "  from(\n"
		        + "          select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "                 greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "                 greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "                 greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "                 d.patient_id as disc_patient,\n"
		        + "                 d.effective_disc_date as effective_disc_date,\n"
		        + "                 max(d.visit_date) as date_discontinued,\n"
		        + "                 de.patient_id as started_on_drugs,\n"
		        + "                 max(if(e.date_started_art_at_transferring_facility is not null and e.facility_transferred_from is not null, 1, 0)) as TI_on_art,\n"
		        + "                 timestampdiff(YEAR, p.DOB, date(:endDate)) as age,\n"
		        + "                 mid(max(concat(fup.visit_date,fup.tb_status)),11) as tb_case\n"
		        + "          from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                   join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                   join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                   join (select c.patient_id,\n"
		        + "                         max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                         m.visit_date          as disc_visit,\n"
		        + "                         m.effective_disc_date as effective_disc_date,\n"
		        + "                         m.patient_id          as disc_client,\n"
		        + "                         p.baby_feeding_method,\n"
		        + "                         a.patient_id          as anc_client\n"
		        + "                       from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                         left join (select p.patient_id,\n"
		        + "                                      max(p.visit_date)                                         as latest_visit,\n"
		        + "                                      mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                    from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                    where p.visit_date <= date(:endDate)\n"
		        + "                                    group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                         left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                    from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                    where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                    group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                         left join (select patient_id,\n"
		        + "                                      max(visit_date) as visit_date,\n"
		        + "                                      mid(\n"
		        + "                                          max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                          11)     as effective_disc_date\n"
		        + "                                    from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                    where date(visit_date) <= date(:endDate)\n"
		        + "                                          and program_name = 'MCH Mother'\n"
		        + "                                    group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                       where c.visit_date <= date(:endDate)\n"
		        + "                             and c.service_type in (1622, 1623)\n"
		        + "                       group by c.patient_id\n"
		        + "                       having (disc_client is null or\n"
		        + "                               (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "              on fup.patient_id = c.patient_id\n"
		        + "                   left outer join (select de.patient_id,min(date(de.date_started)) as date_started, de.program as program from kenyaemr_etl.etl_drug_event de group by de.patient_id) de\n"
		        + "                                   on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "                   left outer JOIN\n"
		        + "               (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "               ) d on d.patient_id = fup.patient_id\n"
		        + "          where fup.visit_date <= date(:endDate)\n"
		        + "          group by patient_id\n"
		        + "          having (started_on_drugs is not null and started_on_drugs <> '' ) and (\n"
		        + "                  (\n"
		        + "                          ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                          and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                      ) and age >= 15) and TI_on_art = 0 and tb_case =142177)t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :S_04_13_Q2 Pregnant or breastfeeding In art Tx Curr, Delivery last 2 weeks
	 * 
	 * @return
	 */
	public static String txCurrPregnantAndBFDeliveryWithin2WeeksQuery() {
		String qry = "select t.patient_id\n"
		        + "from(\n"
		        + "            select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "                             greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "                             greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "                             greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "                             d.patient_id as disc_patient,\n"
		        + "                             d.effective_disc_date as effective_disc_date,\n"
		        + "                             max(d.visit_date) as date_discontinued,\n"
		        + "                             de.patient_id as started_on_drugs\n"
		        + "            from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                join (select ld.patient_id,ld.visit_date\n"
		        + "                            from kenyaemr_etl.etl_mchs_delivery ld\n"
		        + "                            where ld.visit_date between date_sub(date(:endDate), INTERVAL 2 WEEK) and date(:endDate)  group by ld.patient_id) mat on mat.patient_id=fup.patient_id\n"
		        + "                left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "                left outer JOIN\n"
		        + "                (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "                ) d on d.patient_id = fup.patient_id\n"
		        + "            where fup.visit_date <= date(:endDate)\n"
		        + "            group by patient_id\n"
		        + "            having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "                (\n"
		        + "                    ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                    and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                ))order by mat.visit_date desc) t limit 100;";
		return qry;
	}
	
	/**
	 * Cohort definition :S_04_13_Q2 Pregnant or breastfeeding In art Tx Curr, Delivery last 12
	 * Months
	 * 
	 * @return
	 */
	public static String txCurrPregnantAndBreastfeedingDelivery12MonthsQuery() {
		String qry = "select t.patient_id\n"
		        + "from(\n"
		        + "            select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "                         greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "                         greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "                         greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "                         d.patient_id as disc_patient,\n"
		        + "                         d.effective_disc_date as effective_disc_date,\n"
		        + "                         max(d.visit_date) as date_discontinued,\n"
		        + "                         de.patient_id as started_on_drugs\n"
		        + "            from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "                join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + "                join (select ld.patient_id,ld.visit_date\n"
		        + "                            from kenyaemr_etl.etl_mchs_delivery ld\n"
		        + "                            where ld.visit_date between date_sub(date(:endDate), INTERVAL 12 MONTH) and date(:endDate)  group by ld.patient_id) mat on mat.patient_id=fup.patient_id\n"
		        + "                left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n"
		        + "                left outer JOIN\n"
		        + "                (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "                ) d on d.patient_id = fup.patient_id\n"
		        + "            where fup.visit_date <= date(:endDate)\n"
		        + "            group by patient_id\n"
		        + "            having (started_on_drugs is not null and started_on_drugs <> '') and (\n"
		        + "                (\n"
		        + "                    ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                    and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                ))order by mat.visit_date desc) t limit 10;";
		return qry;
	}
	
	/**
	 * S_04_03: Pregnant/Breastfeeding (BF) women on ART >6 month cohort
	 */
	public static String txCurrPregBFOver6MonthsART() {
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                      as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "                d.patient_id                                                           as disc_patient,\n"
		        + "                d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                      as date_discontinued,\n"
		        + "                de.patient_id                                                          as started_on_drugs,\n"
		        + "                max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                       e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "                timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "                de.date_started,\n"
		        + "                c.baby_feeding_method,\n"
		        + "                mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "                c.anc_client,\n"
		        + "                mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  left join (select c.patient_id,\n"
		        + "                               max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                               m.visit_date          as disc_visit,\n"
		        + "                               m.effective_disc_date as effective_disc_date,\n"
		        + "                               m.patient_id          as disc_client,\n"
		        + "                               p.baby_feeding_method,\n"
		        + "                               a.patient_id          as anc_client\n"
		        + "                        from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                 left join (select p.patient_id,\n"
		        + "                                                   max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                   mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                            from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                            where p.visit_date <= date(:endDate)\n"
		        + "                                            group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                 left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                            from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                            where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                            group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                 left join (select patient_id,\n"
		        + "                                                   max(visit_date) as visit_date,\n"
		        + "                                                   mid(\n"
		        + "                                                           max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                           11)     as effective_disc_date\n"
		        + "                                            from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                            where date(visit_date) <= date(:endDate)\n"
		        + "                                              and program_name = 'MCH Mother'\n"
		        + "                                            group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                        where c.visit_date <= date(:endDate)\n"
		        + "                          and c.service_type in (1622, 1623)\n"
		        + "                        group by c.patient_id\n"
		        + "                        having (disc_client is null or\n"
		        + "                                (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                       on fup.patient_id = c.patient_id\n"
		        + "                  left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                   from kenyaemr_etl.etl_drug_event de\n"
		        + "                                   group by de.patient_id) de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '' and\n"
		        + "                 timestampdiff(MONTH, date_started, date(:endDate)) > 6)\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                          date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                 ))\n"
		        + "            and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                 anc_client is not null)\n"
		        + "            and TI_on_art = 0) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * S_04_04: Pregnant/Breastfeeding (BF) women on ART ≥12 months with virologic non-suppression.
	 */
	public static String txCurrPregBFOver12MonthsARTUnsuppressed() {
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                      as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "                d.patient_id                                                           as disc_patient,\n"
		        + "                d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                      as date_discontinued,\n"
		        + "                de.patient_id                                                          as started_on_drugs,\n"
		        + "                max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                       e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "                timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "                de.date_started,\n"
		        + "                c.baby_feeding_method,\n"
		        + "                mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "                c.anc_client,\n"
		        + "                mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  left join (select c.patient_id,\n"
		        + "                                    max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                                    m.visit_date          as disc_visit,\n"
		        + "                                    m.effective_disc_date as effective_disc_date,\n"
		        + "                                    m.patient_id          as disc_client,\n"
		        + "                                    p.baby_feeding_method,\n"
		        + "                                    a.patient_id          as anc_client\n"
		        + "                             from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                      left join (select p.patient_id,\n"
		        + "                                                        max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                        mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                                 from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                                 where p.visit_date <= date(:endDate)\n"
		        + "                                                 group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                      left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                                 from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                                 where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                                 group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                      left join (select patient_id,\n"
		        + "                                                        max(visit_date) as visit_date,\n"
		        + "                                                        mid(\n"
		        + "                                                                max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                                11)     as effective_disc_date\n"
		        + "                                                 from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                                 where date(visit_date) <= date(:endDate)\n"
		        + "                                                   and program_name = 'MCH Mother'\n"
		        + "                                                 group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                             where c.visit_date <= date(:endDate)\n"
		        + "                               and c.service_type in (1622, 1623)\n"
		        + "                             group by c.patient_id\n"
		        + "                             having (disc_client is null or\n"
		        + "                                     (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                            on fup.patient_id = c.patient_id\n"
		        + "                  left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                   from kenyaemr_etl.etl_drug_event de\n"
		        + "                                   group by de.patient_id) de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         inner join (select\n"
		        + "                         b.patient_id,\n"
		        + "                         max(b.visit_date) as vl_date,\n"
		        + "                         date_sub(date(:endDate) , interval 12 MONTH),\n"
		        + "                         mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n"
		        + "                         if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n"
		        + "                         mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n"
		        + "                     from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n"
		        + "                           from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n"
		        + "                           group by x.patient_id,x.visit_date order by visit_date desc)b\n"
		        + "                     group by patient_id\n"
		        + "                     having max(visit_date) between\n"
		        + "                         date_sub(date(:endDate) , interval 12 MONTH) and date(:endDate) and (vl_result >= 1000 and vl_result != 'LDL')) b on fup.patient_id = b.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '' and\n"
		        + "                 timestampdiff(MONTH, date_started, date(:endDate)) >= 12)\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                          date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                 ))\n"
		        + "            and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                 anc_client is not null)\n"
		        + "            and TI_on_art = 0) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * S_04_07:Pregnant/Breastfeeding (BF) women on ART ≥12 months.
	 * 
	 * @return
	 */
	public static String txCurrPregBFOver12MonthsART() {
		String qry = "select t.patient_id\n"
		        + "from (\n"
		        + "         select fup.visit_date,\n"
		        + "                fup.patient_id,\n"
		        + "                max(e.visit_date)                                                      as enroll_date,\n"
		        + "                greatest(max(e.visit_date),\n"
		        + "                         ifnull(max(date(e.transfer_in_date)), '0000-00-00'))          as latest_enrolment_date,\n"
		        + "                greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n"
		        + "                greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n"
		        + "                         ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n"
		        + "                d.patient_id                                                           as disc_patient,\n"
		        + "                d.effective_disc_date                                                  as effective_disc_date,\n"
		        + "                max(d.visit_date)                                                      as date_discontinued,\n"
		        + "                de.patient_id                                                          as started_on_drugs,\n"
		        + "                max(if(e.date_started_art_at_transferring_facility is not null and\n"
		        + "                       e.facility_transferred_from is not null, 1, 0))                 as TI_on_art,\n"
		        + "                timestampdiff(YEAR, p.DOB, date(:endDate))                             as age,\n"
		        + "                de.date_started,\n"
		        + "                c.baby_feeding_method,\n"
		        + "                mid(max(concat(fup.visit_date, fup.pregnancy_status)), 11)             as pregnant,\n"
		        + "                c.anc_client,\n"
		        + "                mid(max(concat(fup.visit_date, fup.breastfeeding)), 11)                as breastfeeding\n"
		        + "         from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n"
		        + "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n"
		        + "                  left join (select c.patient_id,\n"
		        + "                                    max(c.visit_date)     as latest_mch_enrollment,\n"
		        + "                                    m.visit_date          as disc_visit,\n"
		        + "                                    m.effective_disc_date as effective_disc_date,\n"
		        + "                                    m.patient_id          as disc_client,\n"
		        + "                                    p.baby_feeding_method,\n"
		        + "                                    a.patient_id          as anc_client\n"
		        + "                             from kenyaemr_etl.etl_mch_enrollment c\n"
		        + "                                      left join (select p.patient_id,\n"
		        + "                                                        max(p.visit_date)                                         as latest_visit,\n"
		        + "                                                        mid(max(concat(p.visit_date, p.baby_feeding_method)), 11) as baby_feeding_method\n"
		        + "                                                 from kenyaemr_etl.etl_mch_postnatal_visit p\n"
		        + "                                                 where p.visit_date <= date(:endDate)\n"
		        + "                                                 group by p.patient_id) p on p.patient_id = c.patient_id\n"
		        + "                                      left join (select a.patient_id, max(a.visit_date) as latest_visit\n"
		        + "                                                 from kenyaemr_etl.etl_mch_antenatal_visit a\n"
		        + "                                                 where a.visit_date <= date(:endDate) and a.maturity <= 42 or maturity is null\n"
		        + "                                                 group by a.patient_id) a on a.patient_id = c.patient_id\n"
		        + "                                      left join (select patient_id,\n"
		        + "                                                        max(visit_date) as visit_date,\n"
		        + "                                                        mid(\n"
		        + "                                                                max(concat(date(visit_date), date(effective_discontinuation_date))),\n"
		        + "                                                                11)     as effective_disc_date\n"
		        + "                                                 from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                                                 where date(visit_date) <= date(:endDate)\n"
		        + "                                                   and program_name = 'MCH Mother'\n"
		        + "                                                 group by patient_id) m on c.patient_id = m.patient_id\n"
		        + "                             where c.visit_date <= date(:endDate)\n"
		        + "                               and c.service_type in (1622, 1623)\n"
		        + "                             group by c.patient_id\n"
		        + "                             having (disc_client is null or\n"
		        + "                                     (latest_mch_enrollment > coalesce(effective_disc_date, disc_visit)))) c\n"
		        + "                            on fup.patient_id = c.patient_id\n"
		        + "                  left outer join (select de.patient_id,\n"
		        + "                                          min(date(de.date_started)) as date_started,\n"
		        + "                                          de.program                 as program\n"
		        + "                                   from kenyaemr_etl.etl_drug_event de\n"
		        + "                                   group by de.patient_id) de\n"
		        + "                                  on e.patient_id = de.patient_id and de.program = 'HIV' and\n"
		        + "                                     date(date_started) <= date(:endDate)\n"
		        + "                  left outer JOIN\n"
		        + "              (select patient_id,\n"
		        + "                      coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n"
		        + "                      max(date(effective_discontinuation_date)) as               effective_disc_date\n"
		        + "               from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "               where date(visit_date) <= date(:endDate)\n"
		        + "                 and program_name = 'HIV'\n"
		        + "               group by patient_id\n"
		        + "              ) d on d.patient_id = fup.patient_id\n"
		        + "         where fup.visit_date <= date(:endDate)\n"
		        + "         group by patient_id\n"
		        + "         having (started_on_drugs is not null and started_on_drugs <> '' and\n"
		        + "                 timestampdiff(MONTH, date_started, date(:endDate)) >= 12)\n"
		        + "            and (\n"
		        + "             (\n"
		        + "                     ((timestampdiff(DAY, date(latest_tca), date(:endDate)) <= 30 or\n"
		        + "                       timestampdiff(DAY, date(latest_tca), date(curdate())) <= 30) and\n"
		        + "                      ((date(d.effective_disc_date) > date(:endDate) or\n"
		        + "                        date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                     and (date(latest_vis_date) >= date(date_discontinued) or\n"
		        + "                          date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n"
		        + "                 ))\n"
		        + "            and (baby_feeding_method in (5526, 6046) or pregnant = 1065 or breastfeeding = 1065 or\n"
		        + "                 anc_client is not null)\n"
		        + "            and TI_on_art = 0) t order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * S_04_15: HIV-Exposed Infants (HEI): Most recent HIV-infected infants (i.e. born 3 or more
	 * months prior to the SIMS assessment and up to the last 12 months prior to today’s SIMS
	 * assessment)
	 * 
	 * @return
	 */
	public static String newPositiveHEIs3To12MonthsOld() {
		String qry = "select e.patient_id\n"
		        + "from kenyaemr_etl.etl_hei_enrollment e\n"
		        + "         inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = e.patient_id\n"
		        + "    and timestampdiff(MONTH, d.dob, date(:endDate)) between 3 and 12\n"
		        + "         left join (select h.patient_id from kenyaemr_etl.etl_hiv_enrollment h where h.visit_date <= date(:endDate)) h\n"
		        + "                   on e.patient_id = h.patient_id\n"
		        + "where (h.patient_id is not null or (e.exit_reason = 138571 and e.hiv_status_at_exit = 'Positive'))\n"
		        + "order by RAND()\n" + "limit 10;";
		return qry;
	}
	
	/**
	 * HIV-Exposed Infants (HEI): Most recent HIV-infected infants (i.e. born 3 or more months prior
	 * to the SIMS assessment and up to the last 12 months prior to today’s SIMS assessment) who had
	 * an initial positive virologic test result.
	 * 
	 * @return
	 */
	public static String newPosHEIs3To12MonthsOldInitialVirologicResult() {
		String qry = "select e.patient_id\n" + "from kenyaemr_etl.etl_hei_enrollment e\n"
		        + "         inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = e.patient_id\n"
		        + "    and timestampdiff(MONTH, d.dob, date(:endDate)) between 3 and 12\n"
		        + "         left join (select x.patient_id, x.visit_date\n"
		        + "                    from kenyaemr_etl.etl_laboratory_extract x\n"
		        + "                    where x.order_reason = 1040\n" + "                      and x.test_result = 703\n"
		        + "                      and date(x.visit_date) <= date(:endDate)) x on e.patient_id = x.patient_id\n"
		        + "         left join (select v.patient_id\n"
		        + "                    from kenyaemr_etl.etl_hei_follow_up_visit v\n"
		        + "                    where v.visit_date <= date(:endDate)\n"
		        + "                      and (v.dna_pcr_result = 703 and v.dna_pcr_contextual_status = 162080)\n"
		        + "                      and date(v.visit_date) <= date(:endDate)) v\n"
		        + "                   on e.patient_id = v.patient_id\n"
		        + "where (v.patient_id is not null or x.patient_id is not null)\n" + "order by RAND()\n" + "limit 10;";
		return qry;
	}
	
	/**
	 * S_08_02: 10 TB patients diagnosed with HIV more than 3 months but less than 12 months prior
	 * to the SIMS assessment.
	 * 
	 * @return
	 */
	public static String artProvisionForHIVPosAdultTBPatients() {
		String qry = "select a.patient_id from (select dem.patient_id,\n"
		        + "       max(e.visit_date)                                   as latest_enrollment,\n"
		        + "       disc.patient_id                                     as disc_patient,\n"
		        + "       coalesce(disc.effective_disc_date, disc.visit_date) as disc_date\n"
		        + "       from kenyaemr_etl.etl_patient_demographics dem\n"
		        + "inner join kenyaemr_etl.etl_tb_enrollment e  on dem.patient_id = e.patient_id\n"
		        + "         left join (select t.patient_id, t.visit_date as date_tested\n"
		        + "                    from kenyaemr_etl.etl_hts_test t\n"
		        + "                    where t.final_test_result = 'Positive'\n"
		        + "                      and timestampdiff(MONTH, t.visit_date, date(:endDate)) between 3 and 12) t\n"
		        + "                   on t.patient_id = dem.patient_id\n"
		        + "         left join (select h.patient_id,\n"
		        + "                           coalesce(mid(min(concat(h.visit_date, h.date_confirmed_hiv_positive)), 11),\n"
		        + "                                    min(h.visit_date)) as date_confirmed_hiv_pos\n"
		        + "                    from kenyaemr_etl.etl_hiv_enrollment h\n"
		        + "                    where h.visit_date <= date(:endDate) and timestampdiff(MONTH, date_confirmed_hiv_positive, date(:endDate)) between 3 and 12\n"
		        + "                    group by h.patient_id) h\n"
		        + "                   on dem.patient_id = h.patient_id\n"
		        + "         left join (\n"
		        + "    select patient_id,\n"
		        + "           max(visit_date)                                                              as visit_date,\n"
		        + "           mid(max(concat(date(visit_date), date(effective_discontinuation_date))), 11) as effective_disc_date\n"
		        + "    from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "    where date(visit_date) <= date(:endDate)\n" + "      and program_name = 'TB'\n"
		        + "    group by patient_id\n" + ") disc on dem.patient_id = disc.patient_id\n"
		        + "where (t.patient_id is not null or h.patient_id is not null)\n"
		        + " and timestampdiff(YEAR,dem.dob,date(:endDate) >= 18)\n" + "group by e.patient_id\n"
		        + "having disc_patient is null\n" + "    or latest_enrollment >= disc_date  )a\n"
		        + " order by RAND() limit 10;";
		return qry;
	}
	
	/**
	 * S_07_03: 10 clients identified as HIV positive within the last 3 months from the HTS register
	 * to determine the percentage of HIV positive clients who were successfully linked to treatment
	 * services.
	 * 
	 * @return
	 */
	public static String htsLinkageToHIVCareAndTreatment() {
		String qry = "select t.patient_id\n"
		        + "from kenyaemr_etl.etl_hts_test t\n"
		        + "where t.test_type = 2\n"
		        + "  and t.final_test_result = 'Positive'\n"
		        + "  and t.visit_date between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH) and date(:endDate);";
		return qry;
	}
	
	/**
	 * Cohort definition :Evaluator for CohortDefinition: of HEI 3-12 months old started on CTX
	 * 
	 * @return
	 */
	public static String hei3To12MonthsOldOnCTXQuery() {
		String qry = "select e.patient_id\n" + "from kenyaemr_etl.etl_hei_enrollment e\n"
		        + "  inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = e.patient_id\n"
		        + "          and timestampdiff(MONTH, d.dob, date(:endDate)) between 3 and 12\n"
		        + "  join (select v.patient_id\n" + "             from kenyaemr_etl.etl_hei_follow_up_visit v\n"
		        + "             where v.visit_date <= date(:endDate) and (v.ctx_given = 105281)) v\n"
		        + "             on e.patient_id = v.patient_id\n" + "order by RAND()\n" + "limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :Evaluator for CohortDefinition: of HEI 24-36 months old
	 * 
	 * @return
	 */
	public static String hei24To36MonthsOldQuery() {
		String qry = "select e.patient_id\n" + "from kenyaemr_etl.etl_hei_enrollment e\n"
		        + "  inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = e.patient_id\n"
		        + "          and timestampdiff(MONTH, d.dob, date(:endDate)) between 24 and 36\n" + "order by RAND()\n"
		        + "limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition :Evaluator for CohortDefinition: of HEI 3-12 months old
	 * 
	 * @return
	 */
	public static String hei3To12MonthsOldQuery() {
		String qry = "select e.patient_id \n" + "     from kenyaemr_etl.etl_hei_enrollment e \n"
		        + "       inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = e.patient_id \n"
		        + "               and timestampdiff(MONTH, d.dob, date(:endDate)) between 3 and 12\n"
		        + "     order by RAND() \n" + "     limit 10;";
		return qry;
	}
	
	public static String vmmcClientsQuery() {
		String qry = "select e. patient_id from  kenyaemr_etl.etl_vmmc_enrolment e\n"
		        + "order by e.date_created  desc limit 10;";
		return qry;
	}
	
	/**
	 * Cohort definition evaluator of pediatric patients who newly initiated on ART in the last 3
	 * months.
	 */
	public static String pedNewOnARTInLast3Months() {
		String qry = "select net.patient_id  \n"
		        + "from (  \n"
		        + "select e.patient_id,e.date_started,  \n"
		        + "e.gender, \n"
		        + "e.dob, \n"
		        + "d.visit_date as dis_date,  \n"
		        + "if(d.visit_date is not null, 1, 0) as TOut, \n"
		        + "e.regimen, e.regimen_line, e.alternative_regimen,  \n"
		        + "mid(max(concat(fup.visit_date,fup.next_appointment_date)),11) as latest_tca,  \n"
		        + "max(if(enr.date_started_art_at_transferring_facility is not null and enr.facility_transferred_from is not null, 1, 0)) as TI_on_art, \n"
		        + "max(if(enr.transfer_in_date is not null, 1, 0)) as TIn,  \n"
		        + "max(fup.visit_date) as latest_vis_date\n"
		        + "from (select e.patient_id,p.dob,p.Gender,min(e.date_started) as date_started,  \n"
		        + "mid(min(concat(e.date_started,e.regimen_name)),11) as regimen,  \n"
		        + "mid(min(concat(e.date_started,e.regimen_line)),11) as regimen_line,  \n"
		        + "max(if(discontinued,1,0))as alternative_regimen  \n"
		        + "from kenyaemr_etl.etl_drug_event e\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id\n"
		        + "where e.program = 'HIV'\n"
		        + "group by e.patient_id) e  \n"
		        + "left outer join kenyaemr_etl.etl_patient_program_discontinuation d on d.patient_id=e.patient_id and d.program_name='HIV' \n"
		        + "left outer join kenyaemr_etl.etl_hiv_enrollment enr on enr.patient_id=e.patient_id  \n"
		        + "left outer join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id=e.patient_id  \n"
		        + "where date(e.date_started) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH) and date(:endDate)\n"
		        + "and timestampdiff(YEAR ,e.dob,date(:endDate)) <= 15\n"
		        + "group by e.patient_id                 having TI_on_art=0 \n" + ")net order by RAND() limit 10 ;\n";
		return qry;
	}
	
	/**
	 * Cohort definition evaluator of adult and adolescent patients ≥15 years old who newly
	 * initiated ART in the last 3 months .
	 */
	public static String newOnARTInLast3Months() {
		String qry = "select net.patient_id  \n"
		        + "from (  \n"
		        + "select e.patient_id,e.date_started,  \n"
		        + "e.gender, \n"
		        + "e.dob, \n"
		        + "d.visit_date as dis_date,  \n"
		        + "if(d.visit_date is not null, 1, 0) as TOut, \n"
		        + "e.regimen, e.regimen_line, e.alternative_regimen,  \n"
		        + "mid(max(concat(fup.visit_date,fup.next_appointment_date)),11) as latest_tca,  \n"
		        + "max(if(enr.date_started_art_at_transferring_facility is not null and enr.facility_transferred_from is not null, 1, 0)) as TI_on_art, \n"
		        + "max(if(enr.transfer_in_date is not null, 1, 0)) as TIn,  \n"
		        + "max(fup.visit_date) as latest_vis_date\n"
		        + "from (select e.patient_id,p.dob,p.Gender,min(e.date_started) as date_started,  \n"
		        + "mid(min(concat(e.date_started,e.regimen_name)),11) as regimen,  \n"
		        + "mid(min(concat(e.date_started,e.regimen_line)),11) as regimen_line,  \n"
		        + "max(if(discontinued,1,0))as alternative_regimen  \n"
		        + "from kenyaemr_etl.etl_drug_event e\n"
		        + "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id\n"
		        + "where e.program = 'HIV'\n"
		        + "group by e.patient_id) e  \n"
		        + "left outer join kenyaemr_etl.etl_patient_program_discontinuation d on d.patient_id=e.patient_id and d.program_name='HIV' \n"
		        + "left outer join kenyaemr_etl.etl_hiv_enrollment enr on enr.patient_id=e.patient_id  \n"
		        + "left outer join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id=e.patient_id  \n"
		        + "where date(e.date_started) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 3 MONTH) and date(:endDate)\n"
		        + "and timestampdiff(YEAR ,e.dob,date(:endDate)) >= 15\n"
		        + "group by e.patient_id                 having TI_on_art=0 \n" + ")net order by RAND() limit 10 ;\n";
		return qry;
	}
	
	/**
	 * Cohort definition :S_03_11_Q3 In KP program In HIV program In art >= 12 months Age 15 years
	 * and above with a VL result
	 * 
	 * @return
	 */
	public static String txCurrKpMoreThan12MonthsOnArtWithVlQuery() {
		String qry = "select t.patient_id\n"
		        + "  from(\n"
		        + "      select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n"
		        + "             greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n"
		        + "             greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n"
		        + "             greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n"
		        + "             d.patient_id as disc_patient,\n"
		        + "             d.effective_disc_date as effective_disc_date,\n"
		        + "             max(d.visit_date) as date_discontinued,\n"
		        + "             de.patient_id as started_on_drugs,\n"
		        + "   timestampdiff(YEAR ,p.dob,date(:endDate)) as age\n"
		        + "      from kenyaemr_etl.etl_patient_hiv_followup fup\n"
		        + "             join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n"
		        + "             join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n"
		        + " join(\n"
		        + "select c.client_id from kenyaemr_etl.etl_contact c\n"
		        + "left join (select p.client_id from kenyaemr_etl.etl_peer_calendar p where p.voided = 0 group by p.client_id having max(p.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cp on c.client_id=cp.client_id\n"
		        + "left join (select v.client_id from kenyaemr_etl.etl_clinical_visit v where v.voided = 0 group by v.client_id having max(v.visit_date) between date_sub(date_add(date(:endDate), INTERVAL 1 DAY), INTERVAL 12 MONTH)\n"
		        + "and date(:endDate)) cv on c.client_id=cv.client_id\n"
		        + "left join (select d.patient_id, max(d.visit_date) latest_visit from kenyaemr_etl.etl_patient_program_discontinuation d where d.program_name='KP' group by d.patient_id) d on c.client_id = d.patient_id\n"
		        + "where (d.patient_id is null or d.latest_visit > date(:endDate)) and c.voided = 0  and (cp.client_id is not null or cv.client_id is not null) group by c.client_id\n"
		        + ") kp on kp.client_id = fup.patient_id\n"
		        + "     join (\n"
		        + "select x.patient_id, x.visit_date ,x.test_result, x.date_test_requested\n"
		        + "from kenyaemr_etl.etl_laboratory_extract x\n"
		        + "where  lab_test in (856, 1305) and x.date_test_requested <= date(:endDate)\n"
		        + "GROUP BY  x.patient_id\n"
		        + ") l on fup.patient_id = l.patient_id\n"
		        + "           join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV'\n"
		        + " and de.date_started <= date(:endDate) and timestampdiff(MONTH,date(de.date_started), date(:endDate)) >=12\n"
		        + "            left outer JOIN\n"
		        + "               (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n"
		        + "                where date(visit_date) <= date(:endDate) and program_name='HIV'\n"
		        + "                group by patient_id\n"
		        + "               ) d on d.patient_id = fup.patient_id\n"
		        + "      where fup.visit_date <= date(:endDate)\n"
		        + "      group by patient_id\n"
		        + "      having\n"
		        + "          (\n"
		        + "              ((timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 or timestampdiff(DAY,date(latest_tca),date(curdate())) <= 30) and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n"
		        + "                and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null) and age >=15\n"
		        + "              )order by de.date_started desc) t limit 10;";
		return qry;
	}
}