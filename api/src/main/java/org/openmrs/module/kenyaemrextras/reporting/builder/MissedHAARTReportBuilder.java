/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrextras.reporting.builder;

import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractHybridReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyaemr.calculation.library.mchcs.PersonAddressCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.calculation.converter.RDQACalculationResultConverter;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.maternity.*;
import org.openmrs.module.kenyaemrextras.reporting.cohort.definition.rri.MissedHAARTCohortDefinition;
import org.openmrs.module.kenyaemrextras.reporting.data.definition.pmtctRRI.*;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.*;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({ "kenyaemrextras.others.report.missedHAART" })
public class MissedHAARTReportBuilder extends AbstractHybridReportBuilder {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy";
	
	@Override
	protected Mapped<CohortDefinition> buildCohort(HybridReportDescriptor descriptor, PatientDataSetDefinition dsd) {
		dsd.setName("missedHAARTCohort");
		return allPatientsCohort();
	}
	
	protected Mapped<CohortDefinition> allPatientsCohort() {
		CohortDefinition cd = new MissedHAARTCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		return ReportUtils.map(cd, "startDate=${startDate},endDate=${endDate}");
	}
	
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
		
		PatientDataSetDefinition allPatients = missedHAARTDataSetDefinition();
		allPatients.addRowFilter(allPatientsCohort());
		DataSetDefinition allPatientsDSD = allPatients;
		
		return Arrays.asList(ReportUtils.map(allPatientsDSD, "startDate=${startDate},endDate=${endDate}"));
	}
	
	@Override
	protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
		return Arrays.asList(new Parameter("startDate", "Start Date", Date.class), new Parameter("endDate", "End Date",
		        Date.class), new Parameter("dateBasedReporting", "", String.class));
	}
	
	protected PatientDataSetDefinition missedHAARTDataSetDefinition() {
		PatientDataSetDefinition dsd = new PatientDataSetDefinition("missedHAARTCohort");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class,
		    HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
		        upn.getName(), upn), identifierFormatter);
		
		DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName}");
		DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
		PersonAttributeType phoneNumber = MetadataUtils.existing(PersonAttributeType.class,
		    CommonMetadata._PersonAttributeType.TELEPHONE_CONTACT);
		dsd.addColumn("id", new PersonIdDataDefinition(), "");
		
		String paramMapping = "startDate=${startDate},endDate=${endDate}";
		
		dsd.addColumn("Name", nameDef, "");
		dsd.addColumn("Telephone No", new PersonAttributeDataDefinition(phoneNumber), "");
		dsd.addColumn("Village_Estate_Landmark", new CalculationDataDefinition("Village/Estate/Landmark",
		        new PersonAddressCalculation()), "", new RDQACalculationResultConverter());
		
		dsd.addColumn("Age", new AgeDataDefinition(), "");
		dsd.addColumn("Next of kin", new NextOfKinDataDefinition(), "");
		dsd.addColumn("Next of kin phone", new NextOfKinPhoneDataDefinition(), "");
		DateOfLastMCHClinicVisitDataDefinition dateOfLastMCHClinicVisitDataDefinition = new DateOfLastMCHClinicVisitDataDefinition();
		dateOfLastMCHClinicVisitDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dateOfLastMCHClinicVisitDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Date of MCH clinic visit", dateOfLastMCHClinicVisitDataDefinition, paramMapping, null);
		
		MCHDateOfHIVDiagnosisDataDefinition mchDateOfHIVDiagnosisDataDefinition = new MCHDateOfHIVDiagnosisDataDefinition();
		mchDateOfHIVDiagnosisDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		mchDateOfHIVDiagnosisDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Date of HIV diagnosis", mchDateOfHIVDiagnosisDataDefinition, paramMapping, null);
		
		NextMCHVisitAppointmentDateDataDefinition nextMCHVisitAppointmentDateDataDefinition = new NextMCHVisitAppointmentDateDataDefinition();
		nextMCHVisitAppointmentDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		nextMCHVisitAppointmentDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Next appointment date", nextMCHVisitAppointmentDateDataDefinition, paramMapping, null);
		
		ServiceDeliveryPointDataDefinition serviceDeliveryPointDataDefinition = new ServiceDeliveryPointDataDefinition();
		serviceDeliveryPointDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		serviceDeliveryPointDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("Service delivery point", serviceDeliveryPointDataDefinition, paramMapping, null);
		
		return dsd;
	}
	
}
