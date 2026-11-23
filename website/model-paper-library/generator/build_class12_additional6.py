from pathlib import Path
import os, json, random, hashlib, html, re, math, sys, zipfile
import fitz
from weasyprint import HTML
from matplotlib import pyplot as plt
from mpl_toolkits.basemap import Basemap
from io import BytesIO

ROOT=Path(os.environ.get('VISION_CLASS12_ROOT','/mnt/data/vision_class12_expansion'))
PDFROOT=ROOT/'pdfs'
MAPS=ROOT/'maps'
ROOT.mkdir(exist_ok=True);PDFROOT.mkdir(exist_ok=True);MAPS.mkdir(exist_ok=True)
SUBJECTS={
 'Accountancy': {'code':'301','marks':80,'source':'301_copy.pdf','hi':'लेखाशास्त्र'},
 'Business_Studies': {'code':'302','marks':80,'source':'302_copy.pdf','hi':'व्यवसाय अध्ययन'},
 'Economics': {'code':'303','marks':80,'source':'303_copy.pdf','hi':'अर्थशास्त्र'},
 'History': {'code':'101','marks':80,'source':'101_copy.pdf','hi':'इतिहास'},
 'Geography': {'code':'102','marks':70,'source':'102_copy.pdf','hi':'भूगोल'},
 'Political_Science': {'code':'103','marks':80,'source':'103_copy.pdf','hi':'राजनीति विज्ञान'},
}
# Concepts are ORIGINAL educational explanations, not copied from the source papers.
# Short-answer marks correspond to exactly that number of independently meaningful points.
CONCEPTS={
 'Business_Studies': [
 ('Nature and objectives of management','Management means coordinating people and resources to achieve shared goals.', ['It aligns individual activity with agreed organisational objectives.','It improves efficiency by reducing waste and unnecessary duplication.','It is continuous because plans, people and conditions change.','It is multidimensional: work, people and operations are managed together.','Effectiveness means achieving objectives; efficiency means using resources economically.','Managers must balance output, quality, employee wellbeing and long-term responsibility.'],'org'),
 ('Fayol’s unity of command','Each employee should generally receive instructions from one immediate superior.', ['One reporting line reduces conflicting orders.','It clarifies accountability when work is completed or delayed.','It enables a supervisor to assess work consistently.','It does not mean employees cannot cooperate across teams.','A clear organisational chart should show responsibility and authority.','In exceptional matrix teams, dual roles need explicitly agreed reporting arrangements.'],'org'),
 ('Scientific management','Scientific management examines work methods systematically to improve performance.', ['Study tasks rather than relying only on habit or guesswork.','Define a suitable standard method for a recurring operation.','Train workers to use appropriate tools and procedures.','Measure performance with fair, transparent benchmarks.','Cooperate with employees instead of imposing arbitrary targets.','Protect safety and motivation; productivity is not the only important outcome.'],'flow'),
 ('Business environment','Business environment includes external economic, social, legal and technological influences.', ['The economic dimension covers inflation, purchasing power and demand.','The social dimension covers values, tastes and demographics.','The legal dimension covers consumer rights and applicable rules.','Technological change can create opportunities and risks.','Firms should monitor trends before making long-term decisions.','A responsive firm adapts responsibly instead of ignoring the environment.'],'flow'),
 ('Planning process','Planning specifies objectives and chooses a suitable course of action in advance.', ['State a measurable objective and deadline.','Record assumptions about resources and future conditions.','Identify realistic alternative ways of achieving the goal.','Compare costs, benefits and risks of each alternative.','Choose a plan, assign responsibility and communicate it.','Review progress and revise the plan when evidence changes.'],'flow'),
 ('Delegation and decentralisation','Delegation transfers authority to a subordinate for a task while accountability remains with the superior.', ['Assign a well-defined responsibility.','Give enough authority to make the assigned decisions.','Communicate limits and reporting requirements.','Review results; accountability is not automatically transferred.','Decentralisation distributes decision power across multiple organisational levels.','Effective delegation develops staff capacity and reduces managerial overload.'],'org'),
 ('Formal and informal organisation','Formal organisation is the officially defined pattern of jobs, authority and reporting.', ['Formal structures are deliberately designed to meet organisational goals.','Roles and duties can be represented through an organisation chart.','Informal groups emerge naturally through personal interactions.','Informal communication can improve cooperation and speed.','Rumours and exclusion are possible disadvantages of informal networks.','Managers should respect social relationships without relying on rumours as facts.'],'org'),
 ('Recruitment and selection','Recruitment attracts suitable applicants; selection identifies the applicant who best matches the role.', ['Prepare a fair job description and requirements.','Recruit through internal or external sources as appropriate.','Selection uses valid applications, tests or interviews.','Verify role-related evidence consistently for all candidates.','Induct the selected employee and provide necessary training.','Avoid unlawful bias; choose job-relevant criteria.'],'flow'),
 ('Leadership and motivation','Leadership influences people towards a shared goal; motivation encourages sustained effort.', ['Explain a meaningful objective clearly.','Model fair and responsible conduct.','Listen to staff ideas and provide constructive feedback.','Different workers respond to financial and non-financial incentives.','Recognition, autonomy and growth opportunities can support motivation.','Review whether motivation methods are fair and suitable to the workplace.'],'flow'),
 ('Communication barriers','Communication fails when a message is distorted or misunderstood.', ['Semantic barriers arise from unclear words or technical jargon.','Psychological barriers include fear, bias or poor attention.','Organisational barriers include rigid hierarchy and status gaps.','Choose a suitable channel and plain language.','Ask questions and seek feedback to confirm understanding.','Create a respectful environment where clarification is welcomed.'],'flow'),
 ('Controlling process','Control compares actual performance with planned standards and supports corrective action.', ['Set measurable, achievable standards.','Collect reliable data about actual performance.','Compare performance with the standard.','Identify the size and likely cause of deviation.','Correct meaningful deviations rather than punishing small variation.','Use feedback to improve later planning and coordination.'],'flow'),
 ('Financial planning','Financial planning estimates financial needs and identifies suitable sources of funds.', ['Estimate short-term working capital requirements.','Estimate long-term investment requirements.','Choose financing that matches timing and risk.','Compare cost of funds, repayment duties and flexibility.','Maintain a suitable liquidity and solvency balance.','Review cash-flow forecasts and revise assumptions when circumstances change.'],'flow'),
 ('Capital structure','Capital structure is the mix of debt and owners’ equity used to finance business.', ['Debt has contractual interest and repayment obligations.','Equity holders bear residual risk and share residual return.','Excessive debt can raise financial distress risk.','Only equity can avoid fixed debt interest but dilute ownership.','Consider stability of cash flows and costs when choosing the mix.','The preferred mix depends on context, not one universal debt ratio.'],'flow'),
 ('Marketing mix','The marketing mix combines product, price, place and promotion decisions.', ['Product decisions cover quality, features and support.','Price should account for costs, customer value and competition.','Place concerns convenient and efficient distribution channels.','Promotion informs customers through suitable communication.','All four decisions should serve the same target customers.','Ethical marketing avoids misleading claims and unsafe products.'],'flow'),
 ('Consumer rights','Consumers should receive accurate information, safe goods and fair grievance handling.', ['The right to safety concerns protection from hazardous products.','The right to information helps compare quality and cost.','The right to choose supports fair marketplace options.','The right to be heard allows concerns to be considered.','Redressal mechanisms can help resolve justified complaints.','Keep receipts and approach appropriate consumer forums when needed.'],'flow'),
 ('Working capital','Working capital finances day-to-day business operations.', ['Inventory purchases can use cash before the goods are sold.','Credit sales create receivables that take time to collect.','Wages, rent and utilities require regular payment.','Too little working capital can interrupt operations.','Too much idle working capital may lower return.','Cash planning, stock management and collection control help balance needs.'],'flow'),
 ('Advertising and sales promotion','Advertising builds broad awareness; sales promotion gives specific short-term incentives.', ['Advertising is paid and usually addresses a broad audience.','It can explain brand characteristics and product availability.','Sales promotions may include coupons or temporary offers.','Promotional offers should state restrictions accurately.','Excessive promotions can weaken long-term brand positioning.','Effectiveness should be measured using relevant outcomes, not only impressions.'],'flow'),
 ('Staffing and training','Staffing places suitably qualified people into organisational roles.', ['Identify staffing requirements from work plans.','Recruit applicants through appropriate sources.','Select on relevant qualifications and evidence.','Offer induction and job-specific training.','Evaluate performance fairly and provide feedback.','Plan growth and development as technologies and duties change.'],'org'),
 ],
 'Economics': [
 ('Demand and the law of demand','Holding other influences constant, a higher price usually reduces quantity demanded.', ['Demand schedule records quantities buyers plan to purchase at various prices.','A conventional demand curve slopes downwards.','Substitution encourages choosing relatively cheaper alternatives.','Income effects may also change purchasing power.','Changes in income or tastes shift the demand curve itself.','Distinguish movement along a curve from a shift of the whole curve.'],'demand'),
 ('Price elasticity of demand','Price elasticity of demand measures responsiveness of quantity demanded to price.', ['Use percentage change in quantity divided by percentage change in price.','Elasticity is usually presented as an absolute value for comparison.','Demand is elastic when the magnitude exceeds one.','Demand is inelastic when the magnitude is below one.','Availability of substitutes and time for adjustment affect elasticity.','Do not confuse elasticity with the slope of a demand curve.'],'demand'),
 ('Supply and the law of supply','Holding other factors constant, a higher price often encourages a larger quantity supplied.', ['List quantity offered for sale at different prices.','Draw a typical upward-sloping supply curve.','Distinguish own-price changes from technology or input-cost changes.','An input-cost increase can shift supply to the left.','A technology improvement can shift supply to the right.','Market price reflects the interaction of demand and supply.'],'supply'),
 ('Market equilibrium','Market equilibrium occurs where quantity demanded equals quantity supplied.', ['Plot demand and supply on the same price-quantity graph.','Read the intersection as equilibrium price and quantity.','A price above equilibrium tends to produce excess supply.','A price below equilibrium tends to produce excess demand.','Market adjustments depend on how participants respond.','A shift in either curve changes the equilibrium outcome.'],'demand'),
 ('Marginal utility','Marginal utility is the extra satisfaction obtained from consuming an additional unit.', ['Calculate marginal utility as change in total utility.','For a discrete single additional unit, MU = TU(new) - TU(old).','Diminishing marginal utility means successive units often add less satisfaction.','Total utility can rise even while marginal utility falls.','At a maximum of total utility, marginal utility can be zero.','The pattern depends on assumptions and consumer preferences.'],'demand'),
 ('Short-run production costs','Total cost is the sum of total fixed cost and total variable cost.', ['TFC stays constant with output in the short run.','TVC changes with the quantity produced.','TC = TFC + TVC.','Average cost = TC/output for positive output.','Marginal cost is the change in TC for an extra output unit.','A fixed cost is not zero merely because output is zero.'],'cost'),
 ('Perfect competition','Perfect competition assumes many buyers and sellers trading a homogeneous product.', ['No individual firm can change market price by itself.','Firms are price-takers under the model assumptions.','Buyers and sellers have information about prices and products.','Entry and exit barriers are low in the stylised model.','A competitive firm faces a horizontal demand curve at market price.','Real markets may differ, so evaluate which assumptions hold.'],'demand'),
 ('National income accounting','National income aggregates production-related incomes over a defined period.', ['Avoid counting intermediate goods more than once.','Use value added, income or expenditure methods consistently.','State whether measures are at current or constant prices.','GDP counts production within domestic boundaries.','GNP adjusts GDP for net factor income from abroad.','GDP alone does not measure all dimensions of wellbeing.'],'circular'),
 ('Expenditure method of GDP','The expenditure method sums final expenditure on goods and services produced domestically.', ['Consumption expenditure is denoted C.','Investment expenditure is denoted I.','Government final expenditure is G.','Net exports equal exports minus imports, X - M.','GDP = C + I + G + (X - M), with appropriate definitions.','Use consistent time periods and avoid double counting.'],'circular'),
 ('Aggregate demand','Aggregate demand describes planned aggregate spending at each general price level.', ['Household consumption forms an important spending component.','Business investment responds to expectations and financing.','Government expenditure affects demand.','Net exports depend on foreign and domestic conditions.','The aggregate demand curve is distinct from a single-product demand curve.','Output and inflation effects depend on supply conditions as well.'],'circular'),
 ('Money and monetary policy','Money serves as a medium of exchange, unit of account and store of value.', ['Money reduces the need for a double coincidence of wants.','Banks help connect savers and borrowers.','The central bank manages monetary conditions using policy tools.','Policy-rate changes can affect borrowing costs and spending.','Changes operate with lags and are not perfectly predictable.','Distinguish monetary policy from taxation and public spending policy.'],'money'),
 ('Government budget','The government budget reports planned receipts and expenditures.', ['Revenue receipts generally do not create liabilities.','Capital receipts may create liabilities or reduce assets.','Revenue expenditure supports current functioning.','Capital expenditure generally creates assets or reduces liabilities.','A fiscal deficit is total expenditure minus receipts excluding borrowings.','Budget objectives include stability, allocation and redistribution.'],'flow'),
 ('Balance of payments','Balance of payments records economic transactions between residents and the rest of the world.', ['The current account includes goods, services and income flows.','The capital/financial account records relevant asset and liability flows.','Exports and imports of goods form the merchandise trade balance.','A current-account deficit is not automatically the same as overall BOP deficit.','Exchange rates influence cross-border prices and transactions.','Use a consistent accounting convention and time period.'],'circular'),
 ('Multiplier and saving','The simple spending multiplier links a change in autonomous spending to a larger change in income.', ['MPC = change in consumption / change in income.','MPS = 1 - MPC in the simple model.','Multiplier k = 1/(1 - MPC) = 1/MPS, for 0<MPC<1.','Repeated spending rounds shrink if households save part of extra income.','Taxes and imports may reduce the actual multiplier.','State simplifying assumptions when using the formula.'],'circular'),
 ('Development and sustainability','Development considers living standards, capabilities and environmental limits.', ['Economic growth concerns output; development is broader.','Education and health improve human capabilities.','Inequality affects who benefits from growth.','Resource depletion can undermine future livelihoods.','Environmental policy balances costs and long-term benefits.','Use several indicators rather than relying only on per-capita income.'],'flow'),
 ('Foreign exchange rate','An exchange rate states the value of one currency in terms of another.', ['A depreciation makes foreign currency more expensive in domestic units.','An appreciation makes foreign currency cheaper in domestic units.','Other conditions unchanged, depreciation can affect import costs.','Export competitiveness also depends on quality and supply.','Exchange rates respond to supply and demand in relevant markets.','Do not assume currency depreciation benefits every group equally.'],'money'),
 ],
 'Geography': [
 ('Human development','Human development focuses on expanding people’s capabilities and choices.', ['Health improves the ability to lead a long, productive life.','Education develops skills and meaningful participation.','Income can provide access to basic goods and services.','Equity means opportunities should not depend solely on social position.','Sustainability protects possibilities for future generations.','Evaluate development using multiple indicators, not income alone.'],'flow'),
 ('Population distribution','Population distribution is the spatial pattern of where people live.', ['Climate and access to water influence settlement.','Fertile plains support agriculture and dense settlement.','Industry and services create employment clusters.','Transport links improve movement and market access.','Historical and political factors influence migration and location.','Distinguish distribution from numerical population density.'],'map'),
 ('Population density','Population density is number of people per unit area of land.', ['Formula: population / area in square kilometres.','The unit is persons per km².','Choose matching population and area data.','Dense areas may contain large cities or fertile plains.','Low density can reflect aridity, relief or limited infrastructure.','Use regional comparisons carefully because averages hide local differences.'],'map'),
 ('Migration','Migration is movement of people involving a change of usual residence.', ['Push factors include insecurity or limited livelihoods.','Pull factors include jobs, education and services.','Internal migration stays within a country.','International migration crosses an international boundary.','Migration influences sending and receiving areas.','Assess migration with empathy and accurate data, not stereotypes.'],'flow'),
 ('Urbanisation','Urbanisation is an increasing share of people living in urban areas.', ['Industrial and service opportunities can attract workers.','Urban growth increases demand for homes and transport.','Water, sanitation and waste systems require planning.','Unplanned expansion can increase congestion and pollution.','Public transit and inclusive housing support sustainable cities.','Urbanisation rates differ between regions and over time.'],'city'),
 ('Primary, secondary and tertiary activities','Economic activities are often grouped by how goods and services are produced.', ['Primary activities extract or collect natural resources.','Secondary activities transform raw materials into products.','Tertiary activities deliver services such as transport or education.','One business can be connected to more than one sector.','Employment patterns can change with development.','Give specific examples rather than using labels alone.'],'flow'),
 ('Agriculture and irrigation','Irrigation supplements rainfall to supply crops with water.', ['Uneven seasonal rainfall increases the need for irrigation.','Water requirements vary with crop type and growth stage.','Canals, wells and drip systems are different irrigation methods.','Efficient irrigation can reduce avoidable water loss.','Overuse can contribute to groundwater depletion and salinity.','Good management matches crop choice to local water availability.'],'map'),
 ('Mineral resources','Minerals are naturally occurring substances with useful economic properties.', ['Metallic minerals include iron ore and bauxite.','Non-metallic resources include limestone.','Mining can support employment and industrial production.','Deposits are geographically uneven.','Extraction has environmental and social costs.','Rehabilitation and conservation are important during mine planning.'],'map'),
 ('Energy resources','Energy resources power transport, homes and industrial production.', ['Coal and petroleum are finite fossil fuels.','Solar and wind are renewable energy resources.','Hydropower uses flowing or stored water.','Grid stability and storage are important when using variable renewables.','Energy efficiency reduces waste at the consumer level.','Compare full environmental and social impacts of energy options.'],'flow'),
 ('Transport networks','Transport connects producers, markets and settlements.', ['Roads provide flexible local delivery.','Railways suit many bulk and longer-distance movements.','Waterways can be efficient for appropriate cargo.','Air transport is faster but often costlier.','Location, terrain and infrastructure shape transport choice.','Transport planning also considers safety and emissions.'],'flow'),
 ('International trade','International trade involves exchange of goods and services across borders.', ['Export goods and services earn foreign exchange.','Imports supply products or inputs not produced locally.','Comparative advantage can influence trade patterns.','Transport and port networks support trade flows.','Terms of trade and tariffs can affect participants.','Trade benefits and costs may differ across industries and households.'],'map'),
 ('Settlements','Settlements are places where people live and carry out activities.', ['Rural settlements may centre on farming and local services.','Urban settlements offer varied industrial and service jobs.','Site factors include water, relief and accessibility.','Function and size can change over time.','Planned housing and civic services support health.','Avoid assuming all rural or all urban communities are identical.'],'city'),
 ('Environmental pollution','Pollution harms environmental quality through unwanted substances or energy.', ['Air pollution can arise from transport, industry and burning.','Water pollution may result from untreated waste.','Soil pollution can damage productivity and ecosystems.','Prevention is often more effective than later cleanup.','Monitoring helps identify which interventions work.','Community participation and effective regulation both matter.'],'flow'),
 ('Human geography','Human geography studies spatial relationships between people and environments.', ['It examines where populations and activities are located.','It connects social and economic processes with places.','Physical conditions influence but do not wholly determine decisions.','Different societies adapt using technology and organisation.','Maps and statistics help detect spatial patterns.','Use examples to show interaction rather than environmental determinism.'],'map'),
 ('Resource conservation','Resource conservation involves careful use and protection of limited resources.', ['Reduce wastage before increasing extraction.','Re-use and recycling can reduce demand for virgin materials.','Protect water, soil and biodiversity.','Local knowledge and evidence should inform decisions.','Policies must consider communities affected by conservation.','Long-term planning protects opportunities for later generations.'],'flow'),
 ],
 'History': [
 ('Harappan urban planning','Harappan settlements reveal organised streets, drainage and craft activity.', ['Planned streets and buildings indicate coordinated construction.','Drainage systems show attention to urban sanitation.','Craft workshops and artefacts suggest specialised work.','Weights and seals provide evidence of exchange and administration.','Archaeologists infer patterns from material remains, not personal testimony.','Different Harappan sites had different roles and features.'],'map'),
 ('Sources for ancient Indian history','Historians use archaeological and textual evidence to understand past societies.', ['Inscriptions can record donations and political statements.','Coins provide clues about rulers, exchange and symbols.','Buildings and artefacts reveal technologies and practices.','Texts reflect authors, audiences and purposes.','Cross-check sources because each has limits and biases.','Avoid treating one source as a complete account of the past.'],'timeline'),
 ('Mauryan administration','The Mauryan polity used officials and communication networks to govern a large territory.', ['Major centres coordinated governance across regions.','Revenue and resources helped support administration.','Edicts communicate ideas attributed to Ashoka.','Officials conveyed policies to local communities.','Archaeological and textual sources offer different perspectives.','Administrative influence varied from place to place.'],'map'),
 ('Ashoka’s dhamma','Ashoka’s dhamma promoted ethical conduct and concern for others.', ['Inscriptions urge respect across social and religious groups.','They refer to compassion and restraint in behaviour.','Dhamma was not identical to one complete religious doctrine.','The Kalinga war forms part of the historical context.','Edicts show what rulers wanted to communicate, not every actual action.','An exam answer should use inscriptions as evidence and explain limitations.'],'timeline'),
 ('Kinship and social relationships','Ancient texts describe norms for family and social organisation, but practice varied.', ['Patriliny assigns family descent through the father’s line.','Marriage practices were influenced by social norms and local custom.','Prescriptive texts describe ideals, not necessarily universal reality.','Historical narratives can reflect particular social viewpoints.','Property and inheritance practices differed across groups.','Use textual and material evidence together rather than making sweeping claims.'],'timeline'),
 ('Buddhism and Jainism','Buddhism and Jainism developed influential ethical and spiritual traditions.', ['Both engaged with questions about suffering and human conduct.','The Buddha’s teaching discusses the Four Noble Truths.','Jain teachings place strong emphasis on non-violence.','Monasteries and communities helped spread ideas.','Patronage and trade routes influenced religious centres.','Examine inscriptions, architecture and texts as evidence.'],'map'),
 ('Bhakti traditions','Bhakti movements emphasised personal devotion in diverse regional forms.', ['Devotional poetry was composed in several local languages.','Different traditions related to different deities and practices.','Songs and public gatherings enabled wider participation.','Some poets challenged rigid social divisions.','Not all bhakti movements held identical beliefs.','Study the historical setting of each poet or community.'],'timeline'),
 ('Sufi traditions','Sufi traditions developed diverse approaches to spiritual practice and service.', ['Khanqahs often served as centres of teaching and hospitality.','Silsilas connected teachers and disciples.','Music and remembrance held roles in some traditions.','Interactions with local society shaped traditions over time.','Different Sufi orders had distinct practices.','Evidence includes texts, records and shrines.'],'map'),
 ('Vijayanagara empire','Vijayanagara was a major polity with important urban, religious and economic centres.', ['Hampi preserves temples and other architectural remains.','Markets and water systems show extensive planning.','Royal structures reveal public and ceremonial life.','Travellers’ accounts describe aspects of the city.','Imperial influence interacted with local political powers.','Compare archaeological sites with written accounts to avoid overgeneralisation.'],'map'),
 ('Mughal agrarian economy','Agricultural production and land revenue were central to Mughal-era society.', ['Cultivators worked land under varied local arrangements.','Revenue assessment involved estimates of agricultural production.','Zamindars held different roles across regions.','Ain-i-Akbari contains official descriptions and classifications.','Local practice did not always match administrative ideals.','Use revenue records critically alongside other historical evidence.'],'timeline'),
 ('Colonial land revenue policies','British colonial governments introduced varied land revenue settlements.', ['Permanent Settlement fixed revenue demands under specific conditions.','Ryotwari arrangements assessed cultivators in many areas.','Mahalwari arrangements used village-based units in some regions.','Policies affected peasants, intermediaries and land relations.','Outcomes varied with ecology, markets and local power.','A careful answer compares terms and consequences rather than assuming uniform effects.'],'timeline'),
 ('The Revolt of 1857','The Revolt of 1857 involved multiple groups and diverse regional grievances.', ['Military discontent formed part of the outbreak.','Some princes, peasants and townspeople joined in particular regions.','Political annexations and social concerns contributed to resentment.','Participation and objectives differed between places.','British responses reshaped later colonial administration.','Use contemporary reports carefully because colonial and Indian perspectives differ.'],'map'),
 ('Nationalist movement and mass participation','Nationalist politics involved many organisations, leaders and local communities.', ['Ideas about rights and self-government expanded over time.','Non-cooperation encouraged organised non-violent protest in the early 1920s.','Civil disobedience included the Salt March in 1930.','Participation differed by region and social group.','Debates existed about tactics, representation and priorities.','Chronology and regional examples help explain the movement accurately.'],'timeline'),
 ('Gandhi and non-violent protest','Gandhi supported methods of satyagraha that sought change through non-violent action.', ['Identify the specific grievance and intended demand.','Mobilise participation through speeches, meetings and campaigns.','Maintain discipline and avoid targeting individuals with violence.','The Salt March challenged a colonial salt tax in 1930.','Campaigns faced constraints and included internal disagreements.','Evaluate outcomes by comparing aims, participation and political responses.'],'timeline'),
 ('Constituent Assembly','The Constituent Assembly debated principles for independent India’s Constitution.', ['Members discussed rights and democratic governance.','Debates addressed the distribution of public power.','Committees prepared texts for wider discussion.','Multiple viewpoints shaped many provisions.','The Constitution came into effect on 26 January 1950.','Primary debates can reveal why particular choices were made.'],'timeline'),
 ('Historical map interpretation','Map interpretation combines location knowledge with evidence from the period.', ['Read the title, legend and date first.','Identify the region and scale before marking places.','Use known geographic reference points.','Distinguish modern borders from historical political boundaries.','Connect a location to a historical event or site.','Review labels for direction, spelling and factual accuracy.'],'map'),
 ],
 'Political_Science': [
 ('Cold War','The Cold War was a period of sustained rivalry between two major power blocs after World War II.', ['The United States and Soviet Union led competing ideological alignments.','Security competition influenced alliances and military policies.','Many conflicts occurred indirectly through regional partners.','Periods of negotiation coexisted with continuing tension.','Decolonised countries pursued varied foreign-policy approaches.','Avoid describing every regional conflict as caused only by the two powers.'],'timeline'),
 ('Non-Aligned Movement','The Non-Aligned Movement sought independent foreign-policy choices amid Cold War rivalry.', ['Non-alignment did not necessarily mean neutrality on all issues.','Countries varied in their practical priorities.','Respect for sovereignty and anti-colonialism were important themes.','Leaders used diplomatic forums for collective advocacy.','Member states still faced economic and security constraints.','Evaluate policies through specific historical examples rather than labels alone.'],'flow'),
 ('End of Soviet Union','The Soviet Union dissolved in 1991 after prolonged political and economic change.', ['Political reforms changed expectations and competition.','Economic difficulties contributed to public dissatisfaction.','National movements challenged the existing federal structure.','The breakup transformed regional and world politics.','Successor states faced distinct institutional choices.','A multi-causal explanation is more accurate than a single-factor claim.'],'timeline'),
 ('United Nations','The United Nations provides forums for cooperation on peace, rights and development.', ['The General Assembly includes all member states.','The Security Council has specific powers concerning peace and security.','UN agencies support health, education and humanitarian efforts.','Decisions may face political and resource constraints.','International organisations depend on cooperation by states.','Different actors disagree about needed reforms and representation.'],'org'),
 ('Regional organisations','Regional organisations enable neighbouring or linked states to coordinate policy.', ['Common interests may include trade, security or transport.','Institutions differ in membership and decision rules.','Cooperation can lower some transaction costs.','Members may disagree over sovereignty or priorities.','Regional agreements can coexist with global institutions.','Use examples to compare purposes rather than treating all groups as identical.'],'flow'),
 ('Globalisation','Globalisation increases cross-border connections in production, communication and culture.', ['Technology reduces some communication and coordination costs.','Trade and finance connect national economies.','Cultural exchange may increase access to new ideas.','Benefits vary by sector, employment and access to resources.','Governments regulate some cross-border activity.','Balanced answers should address opportunities and unequal effects.'],'flow'),
 ('Environmental cooperation','Environmental challenges often cross national boundaries and need coordination.', ['Climate change involves global greenhouse-gas emissions.','Biodiversity loss and pollution can affect multiple states.','Different countries have unequal historic emissions and resources.','Agreements depend on monitoring and shared commitments.','Local action remains important alongside international negotiation.','Consider equity and effectiveness rather than blaming only one country.'],'flow'),
 ('India’s foreign policy','India’s foreign policy combines interests, principles and changing international conditions.', ['Sovereignty and security are longstanding concerns.','Economic links influence diplomatic choices.','Participation in international forums enables dialogue.','Relationships with neighbours are affected by geography and history.','Policies respond to changing governments and global conditions.','Separate established historical facts from current political opinion.'],'map'),
 ('Indian parliamentary democracy','Parliamentary democracy links executive accountability to an elected legislature.', ['Citizens choose representatives through elections.','The executive is generally accountable to the elected house.','Parliament debates laws and public policy.','Opposition parties scrutinise government decisions.','Courts and constitutional rules provide additional checks.','Democratic accountability also depends on informed public participation.'],'org'),
 ('Coalition governments','Coalition governments bring parties together when no single party governs alone.', ['Parties negotiate a programme or shared priorities.','Coalitions require ongoing compromise and communication.','Different groups can gain representation in decisions.','Disagreements can make coordination harder.','Institutions and rules structure how majority support is maintained.','Judge effectiveness from policy and accountability, not party count alone.'],'org'),
 ('Federalism','Federalism distributes authority between different levels of government.', ['A constitution allocates subjects and powers.','State governments handle important regional responsibilities.','National institutions manage matters assigned nationally.','Financial and administrative cooperation links levels.','Dispute-resolution mechanisms help handle disagreements.','Decentralisation can improve local responsiveness when implemented well.'],'org'),
 ('Social movements','Social movements organise collective action around public concerns.', ['Participants identify a shared issue or grievance.','Organisations communicate demands to institutions.','Campaigns may use petitions, meetings or peaceful demonstrations.','Movements differ in leadership and methods.','Government and public responses affect outcomes.','Evaluate movements using their aims, evidence and broader consequences.'],'flow'),
 ('Democratic elections','Elections choose representatives and help make leaders accountable.', ['Voters need accessible and reliable information.','Clear election rules support fairness and participation.','Secret ballots protect voter choice.','Independent institutions administer election processes.','Responsible public debate allows scrutiny of policies.','Electoral outcomes are one part of ongoing democratic accountability.'],'org'),
 ('Nation-building challenges','Nation-building after independence involved maintaining unity while respecting diversity.', ['Partition created major humanitarian and administrative challenges.','Integration of princely states required negotiation and policy.','Language and regional identity influenced state reorganisation.','Democratic institutions needed to develop public trust.','Economic inequalities remained important policy questions.','Avoid treating diverse communities as a single viewpoint.'],'timeline'),
 ('Human rights','Human rights describe basic entitlements connected to human dignity.', ['Civil and political rights concern freedoms and public participation.','Economic and social rights include welfare-related guarantees.','Rights protections require laws and functioning institutions.','Responsibilities and lawful restrictions can raise debates.','International agreements influence discussion and standards.','A good answer uses precise examples and avoids slogans alone.'],'flow'),
 ('Political cartoons as sources','A political cartoon is an interpretive primary source, not an objective photograph.', ['Identify symbols, labels and the approximate date.','Describe only what is visible before interpreting its meaning.','Explain the viewpoint being expressed by the artist.','Consider alternative interpretations where evidence allows.','Check the historical events behind its references.','Do not treat satire as proof of a factual allegation.'],'timeline'),
 ],
}

# Separate small objective banks; answers have explicit reasons.
OBJECTIVES={
 'Business_Studies':[
 ('Which management level usually coordinates departments and translates strategic plans into operations?', ['Top management','Middle management','Shareholders only','External auditors'],1,'Middle managers connect strategic direction with departmental implementation.'),
 ('Which Fayol principle states that one employee should ordinarily report to one superior?', ['Division of work','Equity','Unity of command','Initiative'],2,'Unity of command reduces conflicting directions.'),
 ('Which is an internal recruitment source?', ['Campus hiring','Transfer of an existing employee','Public advertisement','Employment exchange'],1,'An internal transfer uses workers already employed by the enterprise.'),
 ('Which is a financial incentive?', ['Job enlargement','Recognition certificate','Cash performance bonus','Participation in decision-making'],2,'A cash bonus provides a direct financial reward.'),
 ('The four Ps of marketing are product, price, place and ...?', ['Planning','Promotion','Profit','Process control'],1,'Promotion is the communication component of the traditional marketing mix.'),
 ('Which function sets standards and checks results against them?', ['Directing','Staffing','Controlling','Promoting'],2,'Control compares actual performance with desired standards.'),
 ('Which consumer right helps compare products using accurate labels?', ['Right to information','Right to litter','Right to evade payment','Right to monopoly'],0,'Information about content, price and conditions supports informed choice.'),
 ('Who usually bears fixed interest obligations in financing?', ['Equity share capital','Debt capital','Retained earnings only','Unpaid customer order'],1,'Debt agreements can require interest and repayment.'),
 ('A network of informal employee relationships is called ...?', ['Line authority','Informal organisation','Only delegation','External recruitment'],1,'Informal organisation emerges through personal interactions.'),
 ('A plan chosen after comparing several alternatives illustrates ...?', ['Planning','Recruitment','Merchandising','Accounting'],0,'Planning evaluates routes before action.'),
 ],
 'Economics':[
 ('Which formula expresses total cost in the short run?', ['TC=TFC+TVC','TC=AR-MR','TC=GDP+MPS','TC=Price/Quantity'],0,'Total cost consists of fixed and variable components.'),
 ('When demand equals supply, the market is in ...?', ['Excess demand','Excess supply','Equilibrium','Shortage only'],2,'At equilibrium intended purchase and sale quantities coincide.'),
 ('MPC + MPS equals ... in the simple two-use income model?', ['0','1','2','100'],1,'Each extra rupee is allocated between consumption and saving.'),
 ('Which is included in aggregate expenditure GDP?', ['Intermediate purchases twice','C+I+G+(X-M)','Only imports','Only taxes'],1,'The formula sums final spending and net exports.'),
 ('Which institution is responsible for India’s monetary policy?', ['RBI','Gram Sabha','Only commercial banks','SEBI alone'],0,'India’s central bank conducts monetary policy within its statutory framework.'),
 ('An upward-sloping supply curve typically shows ...?', ['Higher price, larger quantity supplied','Lower price, larger quantity supplied','Fixed demand','No seller choice'],0,'Producers usually offer more at higher own prices, other factors unchanged.'),
 ('A price rise from ₹10 to ₹12 and demand fall from 100 to 80 is a ...?', ['Movement along demand','Increase in supply','Shift due to tastes','Change in population'],0,'Only own-price change causes movement along an unchanged demand curve.'),
 ('Money serves as ...?', ['Medium of exchange','Only a raw material','Only natural resource','Only final output'],0,'Money helps settle transactions without barter.'),
 ('What is a fiscal deficit?', ['Government revenue receipts only','Total expenditure minus receipts excluding borrowings','Only bank deposits','Net exports'],1,'Borrowing-financed expenditure is reflected in fiscal deficit under the stated accounting convention.'),
 ('GDP refers primarily to production ...?', ['By residents abroad only','Within domestic territory','In a single market stall','By one sector only'],1,'Domestic product is measured with reference to economic territory.'),
 ],
 'Geography':[
 ('Population density means ...?', ['People per unit area','Only total population','Only rate of births','Number of cities'],0,'Density divides population by area.'),
 ('Which is a tertiary activity?', ['Mining ore','Transport services','Cultivating rice','Extracting coal'],1,'Transport provides a service.'),
 ('Which is a renewable energy source?', ['Petroleum','Coal','Solar radiation','Natural gas'],2,'Solar energy is replenished naturally.'),
 ('Which transport mode usually carries urgent international passengers fastest?', ['Airways','Bullock cart','Inland waterway','Walking'],0,'Air transport offers high speed over long distances.'),
 ('Which is a push factor for migration?', ['New job offer at destination','Local unemployment','University admission','Better hospital in a city'],1,'Lack of livelihoods can push movement away from an area.'),
 ('A map legend explains ...?', ['Map symbols','Only election results','Only elevation','Only latitude'],0,'A legend states what colours and marks represent.'),
 ('Which crop is commonly associated with plantation agriculture?', ['Tea','Wheat only','Millet only','Pulses only'],0,'Tea is a major plantation crop.'),
 ('A rural settlement is often closely linked to ...?', ['Agriculture and local resource use','Only stock exchanges','Only international airports','Only software services'],0,'Many rural settlements depend on primary activity and services.'),
 ('Which water-saving irrigation method delivers water near plant roots?', ['Drip irrigation','Flooding only','Open dumping','Sea navigation'],0,'Drip systems can reduce evaporation and runoff losses.'),
 ('A major risk of excessive groundwater pumping is ...?', ['Aquifer depletion','More natural rainfall','Instant soil creation','No environmental impact'],0,'Withdrawal faster than recharge can lower groundwater levels.'),
 ],
 'History':[
 ('The Harappan civilisation is especially known for ...?', ['Organised urban settlements','Modern aircraft','Steam railways','Printing press'],0,'Excavations show planned streets and drainage at several sites.'),
 ('Ashoka’s public edicts are a type of ...?', ['Inscription','Modern newspaper','Satellite record','E-mail'],0,'Engraved inscriptions communicate official statements.'),
 ('The Salt March is associated with ...?', ['1930','1757','1857','1947'],0,'Gandhi led the Salt March during the Civil Disobedience movement in 1930.'),
 ('Hampi is associated particularly with ...?', ['Vijayanagara','Harappan harbour','Portuguese factory only','1857 Delhi'],0,'Hampi preserves major remains of Vijayanagara.'),
 ('The Indian Constitution took effect on ...?', ['26 January 1950','15 August 1947','26 January 1930','2 October 1942'],0,'The Constitution was brought into force on 26 January 1950.'),
 ('Sanchi is widely associated with which monument type?', ['Buddhist stupas','Mughal forts','Colonial railway stations','Harappan docks'],0,'Sanchi has prominent Buddhist stupas.'),
 ('Ain-i-Akbari describes features of ...?', ['Akbar-era Mughal administration','Modern computer accounting','Mauryan edicts','Harappan writing only'],0,'It is an important source about Mughal administration and society.'),
 ('Which is an archaeological source?', ['Pottery shard','Modern gossip alone','Opinion poll','Online advertisement'],0,'Material remains such as pottery are archaeological evidence.'),
 ('Patriliny generally means descent traced through ...?', ['Father’s line','Only mother’s line','No family relation','Only occupation'],0,'The term refers to descent through the father’s side.'),
 ('Which event is generally dated to 1857?', ['Major anti-colonial revolt','Salt March','Constitution adoption','Partition of Bengal in 1905'],0,'The Revolt of 1857 involved military and civilian participants across regions.'),
 ],
 'Political_Science':[
 ('The Cold War rivalry chiefly involved which two superpowers?', ['USA and USSR','India and Nepal','Brazil and Chile','Japan and Australia'],0,'The USA and USSR led opposing Cold War blocs.'),
 ('The Soviet Union dissolved in ...?', ['1991','1945','1950','2008'],0,'Its dissolution in 1991 transformed the international order.'),
 ('Non-alignment primarily emphasised ...?', ['Independent foreign policy','Mandatory membership in one bloc','No diplomatic relations','No economic policy'],0,'Non-alignment sought room for sovereign policy choices.'),
 ('Which UN body includes every UN member state?', ['General Assembly','One national parliament','Security Council permanent five only','World Bank board'],0,'All member states belong to the General Assembly.'),
 ('Federalism involves ...?', ['Division of public authority across levels','A single local school','Only one private company','Elimination of states'],0,'Federal systems allocate responsibilities across different government levels.'),
 ('A coalition government is formed by ...?', ['Multiple cooperating parties','One judge alone','Only trade unions','Only the civil service'],0,'Coalitions involve agreement among political parties to govern.'),
 ('Which is a civil or political right?', ['Freedom of expression','Only factory output','Only export volume','Only market price'],0,'Civil-political rights protect individual freedom and participation.'),
 ('What should a student do first with a political cartoon?', ['Describe visible symbols and date','Assume it is a photograph','Accept a slogan as proof','Ignore all labels'],0,'Separating observation from interpretation reduces factual mistakes.'),
 ('An election is one method for ...?', ['Choosing representatives','Guaranteeing identical opinions','Ending all debate','Replacing every public institution'],0,'Elections provide a constitutional path for representative choice.'),
 ('Globalisation refers to growing ...?', ['Cross-border interconnection','Complete separation of economies','Elimination of culture','Only village self-sufficiency'],0,'Economic, technological and cultural links span national boundaries.'),
 ],
 'Accountancy':[
 ('Goodwill','Goodwill represents the value associated with a business’s established reputation and customer relationships.', ['Accountancy uses specified valuation methods for goodwill in partnership changes.','Average-profit and super-profit methods depend on different assumptions.','For average profit, goodwill = average maintainable profit × years’ purchase.','In partner admission, goodwill adjustment depends on the new profit-sharing ratio.','Use the agreed accounting treatment and show calculations transparently.','Avoid valuing goodwill simply as all recorded assets minus liabilities.'],'ledger'),
 ('Profit-sharing ratio','A partnership agreement may specify how profits and losses are shared.', ['Write each partner’s agreed fraction or ratio.','Check that fractions sum to one.','Multiply the net profit by each partner’s fraction.','Account for interest, salary and other appropriations if the agreement provides them.','Record the appropriation entries in the correct accounts.','Confirm total partner shares equal the distributable profit.'],'ledger'),
 ('Sacrificing ratio','Sacrificing ratio measures the share of profit existing partners give up to admit a new partner.', ['Record each existing partner’s old profit share.','Determine each partner’s new profit share.','Sacrifice = old share - new share.','Compare positive sacrifices to calculate the ratio.','Use the ratio for goodwill adjustment if applicable.','If a partner gains rather than sacrifices, distinguish the gaining share.'],'ledger'),
 ('Revaluation account','A revaluation account records changes in book values of assets and liabilities during partnership reconstitution.', ['Record increases in assets on the credit side.','Record decreases in assets on the debit side.','Record increased liabilities on the debit side.','Record decreased liabilities on the credit side.','Find profit or loss as the account balance.','Distribute revaluation profit/loss in the old profit-sharing ratio.'],'ledger'),
 ('Partner admission','Admission of a new partner changes the profit-sharing arrangement and may require goodwill adjustments.', ['Determine the new ratio from the agreement.','Calculate sacrificing ratio of existing partners.','Apply the chosen goodwill treatment.','Revalue assets and liabilities when required.','Adjust reserves and accumulated profits according to instructions.','Prepare revised capital accounts and a balance sheet.'],'ledger'),
 ('Retirement of a partner','When a partner retires, accounts are adjusted to settle amounts due.', ['Determine the gaining ratio for continuing partners.','Calculate goodwill adjustments when relevant.','Share revaluation results and undistributed reserves.','Compute retiring partner’s capital balance.','Settle cash or transfer balance to a loan account as agreed.','Record all journal entries using the agreement and applicable rules.'],'ledger'),
 ('Dissolution','Firm dissolution involves realisation of assets and settlement of liabilities.', ['Transfer relevant assets to a realisation account.','Transfer outside liabilities according to accounting rules.','Record sale proceeds and settlement amounts.','Calculate profit or loss on realisation.','Distribute the result among partners using their ratio.','Settle capital accounts and close cash/bank account.'],'ledger'),
 ('Issue of shares at par','Shares issued at par are offered for their face value.', ['Record application money received.','Transfer allotted application money to share capital.','Record allotment and calls when due.','Record receipts of instalments.','Identify unpaid amounts or excess subscriptions where applicable.','Ensure total recorded share capital matches allotted face value.'],'ledger'),
 ('Shares at premium','Shares may be issued above face value; the excess is securities premium.', ['Face value and issue price must be distinguished.','Premium = issue price - face value.','Calculate total premium based on shares issued.','Credit the securities premium account appropriately.','Record application, allotment and call receipts separately.','Use premium in accordance with applicable company law rules.'],'ledger'),
 ('Debentures','Debentures represent a form of borrowed capital with repayment terms.', ['Identify nominal value and issue price.','Calculate amount received from issue.','Differentiate issue at par, discount and premium.','Record finance costs and redemption obligations as applicable.','Presentation depends on accounting standards and terms.','Reconcile amounts and show the journal entries.'],'ledger'),
 ('Current and quick ratios','Liquidity ratios examine ability to meet short-term obligations.', ['Current ratio = current assets / current liabilities.','Quick assets normally exclude inventory and prepaid expenses.','Quick ratio = quick assets / current liabilities.','Insert figures with the same currency units.','Interpret the results using industry and prior-period context.','A ratio alone does not guarantee cash is available to pay bills.'],'ledger'),
 ('Cash flow statement','A cash flow statement groups actual cash movements by activity.', ['Operating activities relate primarily to core business.','Investing activities include relevant asset purchases and sales.','Financing activities involve capital and borrowing transactions.','Non-cash transactions should not be shown as cash movements.','Reconcile opening and closing cash and cash equivalents.','Use consistent categories and verify totals.'],'ledger'),
 ('Comparative statements','Comparative financial statements show changes across two accounting periods.', ['Align items with the same definitions in each year.','Absolute change = current amount - base amount.','Percentage change = absolute change / base amount × 100.','Use the correct base period in the denominator.','Comment on direction and possible reasons without overclaiming.','Combine with other information before drawing conclusions.'],'ledger'),
 ('Computerised accounting','Computerised accounting systems process financial transactions using controlled software.', ['Correct transaction entry and chart of accounts remain important.','Validation helps reduce but cannot eliminate user errors.','Backups and access controls protect records.','Audit trails support transaction review.','Accurate reports depend on source data quality.','Follow privacy and security policies for accounting information.'],'flow'),
 ('Inventory valuation','Inventory valuation assigns a cost to closing stock under a stated method.', ['Identify opening stock, purchases and costs.','Apply a recognised cost method consistently.','Separate purchases from expenses not included in inventory cost.','Compare with net realisable value if required by applicable standards.','Calculate cost of goods sold accordingly.','Explain how different methods may affect reported profit.'],'ledger'),
 ],
}

# Accountancy concepts were defined together with the fact bank for source clarity.
CONCEPTS['Accountancy'] = OBJECTIVES.pop('Accountancy')
OBJECTIVES['Accountancy'] = [
 ('How is goodwill calculated under the average-profit method?', ['Average profit × years purchase','Total assets + liabilities','Cash receipts − payments','Only market value of land'],0,'Goodwill is the product of maintainable average profit and purchase multiple under this method.'),
 ('The profit-sharing ratio in a partnership determines ...?', ['Division of agreed profit','Only office rent','Only sales tax','Only purchase price'],0,'The partnership agreement determines the allocation of profits and losses.'),
 ('The sacrificing ratio is used especially when ...?', ['A partner is admitted','Only a utility bill is paid','Goods are shipped','Cash is deposited in a bank'],0,'Incoming partner goodwill is often adjusted using existing partners sacrificing proportions.'),
 ('A current ratio is calculated as ...?', ['Current assets / current liabilities','Current liabilities / equity','Revenue / debt','Cash / long-term loans'],0,'Current assets divided by current liabilities gives the standard liquidity ratio.'),
 ('An issue of shares at par means issue price equals ...?', ['Face value','Double face value','Zero','Only book value of company'],0,'At par means issue price equals nominal face value.'),
 ('Which is a financing activity in a cash-flow statement?', ['Issuing shares for cash','Selling inventory','Paying wages','Buying paper for resale'],0,'Capital raised from share issue is a financing cash flow.'),
 ('A debenture is generally ...?', ['Borrowed capital','Only ordinary equity','An inventory stock','A prepaid expense'],0,'Debentures are generally debt instruments with contractual financing conditions.'),
 ('In the journal, total debit should equal ...?', ['Total credit','Only cash sales','Only profit','Interest percentage'],0,'Double-entry bookkeeping requires equal debit and credit totals.'),
 ('Premium on issue of shares is usually credited to ...?', ['Securities Premium','Purchases','Rent Expense','Depreciation'],0,'Premium is accounted for separately from face-value share capital.'),
 ('Revaluation profit on partner admission is normally shared by ...?', ['Old partners in old ratio','Only incoming partner','All customers','Only the creditor'],0,'Revaluation relates to values before reconstitution and normally belongs to old partners.'),
]

# Numericals are parametrised by set number; every instance shows its working.
def numerical(sub,st,idx,mark):
    v=st
    if sub=='Accountancy':
        n=idx%9
        if n==0:
            total=(36+v*4)*1000; a=3+v%3; b=2+((v+1)%2)
            share=total*a/(a+b)
            return (f'A and B share profit in {a}:{b}. Their distributable profit is ₹{total:,}. Calculate A’s share and show a check.',
               [f'Write total ratio units = {a}+{b} = {a+b}.',f'A’s fraction = {a}/{a+b}.',f'A’s share = ₹{total:,} × {a}/{a+b} = ₹{share:,.0f}.',f'B’s share = ₹{total-share:,.0f}.',f'Check: ₹{share:,.0f} + ₹{total-share:,.0f} = ₹{total:,}.','Interpretation: the profit is shared according to the agreement.'])
        if n==1:
            assets=(120+v*15)*1000; liabilities=(60+v*4)*1000; inv=(30+v*3)*1000
            return (f'Current assets ₹{assets:,}, inventory ₹{inv:,}, current liabilities ₹{liabilities:,}. Calculate current and quick ratios.',
               ['Formula: Current ratio = current assets / current liabilities.',f'Substitute: {assets:,}/{liabilities:,} = {assets/liabilities:.2f}:1.',f'Quick assets = current assets − inventory = ₹{assets-inv:,}.',f'Quick ratio = {assets-inv:,}/{liabilities:,} = {(assets-inv)/liabilities:.2f}:1.','State results as ratios, not percentages.','These ratios indicate short-term liquidity; interpretation requires context.'])
        if n==2:
            goodwill=(75+v*5)*1000; frac=2+v%3; share=goodwill/frac
            return (f'Goodwill of a firm is ₹{goodwill:,}. A new partner receives 1/{frac} of profits. Find the partner’s share of goodwill.',
               ['Identify the agreed total goodwill.',f'New partner’s fraction = 1/{frac}.',f'Goodwill share = ₹{goodwill:,} × 1/{frac}.',f'Calculate = ₹{share:,.0f}.','Adjust sacrificing partners’ accounts using the agreed sacrificing ratio.','Do not assume the whole goodwill is paid to the new partner.'])
        if n==3:
            face=100; count=500+v*100; price=90+v; cash=count*price; discount=count*(face-price)
            return (f'Issue {count} debentures of ₹{face} each at ₹{price}. Calculate cash received and issue discount.',
               [f'Face value total = {count} × ₹{face} = ₹{count*face:,}.',f'Issue proceeds = {count} × ₹{price} = ₹{cash:,}.',f'Discount per debenture = ₹{face-price}.',f'Total issue discount = {count} × ₹{face-price} = ₹{discount:,}.','Accounting presentation depends on applicable rules and instrument terms.','Check: cash received plus discount equals total face value.'])
        if n==4:
            old=50000+v*5000; new=old+8000+v*1000; diff=new-old
            return (f'Opening fixed asset value is ₹{old:,}, and closing value ₹{new:,}. Calculate absolute and percentage change.',
               [f'Absolute change = closing − opening = ₹{new:,} − ₹{old:,}.',f'Change = ₹{diff:,}.',f'Percentage change = {diff:,}/{old:,} × 100.',f'Percentage increase = {diff/old*100:.2f}%.','Use opening value, not closing value, as the base.','A change in value alone does not identify the cause.'])
        if n==5:
            profit=(80+v*5)*1000; years=2+v%3
            return (f'Average maintainable profit = ₹{profit:,}; years’ purchase = {years}. Estimate goodwill using average-profit method.',
               ['Formula: goodwill = average maintainable profit × years’ purchase.',f'Insert the data: ₹{profit:,} × {years}.',f'Goodwill = ₹{profit*years:,}.','This method assumes the chosen maintainable profit and purchase multiple are appropriate.','Explain that goodwill is not cash or an ordinary physical asset.','Check the calculation and disclose the method.'])
        if n==6:
            debt=(90+v*10)*1000; eq=(150+v*20)*1000
            return (f'Long-term debt ₹{debt:,}; shareholders’ funds ₹{eq:,}. Find debt-equity ratio and explain.',
               ['Formula = long-term debt / shareholders’ funds.',f'Substitute = {debt:,}/{eq:,}.',f'Ratio = {debt/eq:.2f}:1.','Debt introduces fixed financing obligations.','Compare with past data and industry before judging safety.','Ratio alone cannot show cash available for repayments.'])
        if n==7:
            total=(10+v*2)*1000; c=3+v%3; d=2
            return (f'Partners C and D share a revaluation profit of ₹{total:,} in {c}:{d}. Allocate the credit to their capital accounts.',
               [f'Total units = {c}+{d} = {c+d}.',f'C’s share = ₹{total:,} × {c}/{c+d} = ₹{total*c/(c+d):,.0f}.',f'D’s share = ₹{total:,} × {d}/{c+d} = ₹{total*d/(c+d):,.0f}.','Credit each partner’s capital account with their share.','Debit the revaluation account for the total allocated profit.','Check both amounts sum to the original profit (rounding to nearest rupee where needed).'])
        n=750+v*50; face=10; premium=2+v%3
        return (f'A company issues {n} shares of face value ₹{face} each at ₹{premium} premium. Find share capital and securities premium.',
          [f'Face-value capital = {n} × ₹{face} = ₹{n*face:,}.',f'Premium per share = ₹{premium}.',f'Total premium = {n} × ₹{premium} = ₹{n*premium:,}.',f'Total cash expected = ₹{n*(face+premium):,}.','Credit equity share capital and securities premium separately.','Check the two credits together equal total issue proceeds.'])
    if sub=='Economics':
        n=idx%7
        if n==0:
            old=120+v*20; new=old+15+v
            return (f'Total utility rises from {old} utils to {new} utils after one more unit. Find marginal utility.',
               ['MU = change in total utility / change in units.',f'Change = {new} - {old} = {new-old} utils.','One additional unit was consumed, so denominator = 1.',f'MU = {new-old}/1 = {new-old} utils.','This is marginal, not average, utility.','Check by adding MU to initial TU to recover new TU.'])
        if n==1:
            mpc=.5+v*.05
            return (f'In a simple model MPC = {mpc:.2f}. Calculate MPS and the expenditure multiplier.',
               ['MPS = 1 − MPC.',f'MPS = 1 − {mpc:.2f} = {1-mpc:.2f}.','Simple spending multiplier k = 1/(1 − MPC).',f'k = 1/{1-mpc:.2f} = {1/(1-mpc):.2f}.','Assume unchanged prices, taxes and imports for this basic model.','Check: MPC + MPS = 1.'])
        if n==2:
            fixed=100+v*20; variable=220+v*30; quantity=10+v*2
            return (f'Fixed cost ₹{fixed}, variable cost ₹{variable}, output {quantity} units. Calculate TC, AC and AFC.',
               [f'TC = TFC + TVC = ₹{fixed}+₹{variable} = ₹{fixed+variable}.',f'AC = TC/output = {fixed+variable}/{quantity} = ₹{(fixed+variable)/quantity:.2f} per unit.',f'AFC = TFC/output = {fixed}/{quantity} = ₹{fixed/quantity:.2f} per unit.','Use output as denominator only when output is positive.','Do not confuse average cost with marginal cost.','Check: all amounts use the same period.'])
        if n==3:
            c=500+v*40; inv=200+v*20; gov=150+v*30; x=120+v*10; imp=90+v*5; total=c+inv+gov+x-imp
            return (f'An economy records C={c}, I={inv}, G={gov}, exports={x}, imports={imp} (₹ crore). Find GDP by expenditure.',
               ['Formula: GDP = C + I + G + (X − M).',f'Net exports = {x} − {imp} = {x-imp} crore.',f'Substitute: {c}+{inv}+{gov}+{x-imp}.',f'GDP = ₹{total:,} crore.','Include only appropriate final expenditure.','Double counting of intermediate goods must be avoided.'])
        if n==4:
            p0=20; p1=22+v; q0=100+v*5; q1=q0-(10+v*2)
            ep=((q1-q0)/q0)/((p1-p0)/p0)
            return (f'Price rises ₹{p0}→₹{p1}, quantity demanded falls {q0}→{q1}. Find approximate simple percentage-change price elasticity.',
               [f'Percentage quantity change = ({q1}-{q0})/{q0} ×100 = {(q1-q0)/q0*100:.2f}%.',f'Percentage price change = ({p1}-{p0})/{p0} ×100 = {(p1-p0)/p0*100:.2f}%.','Ed = %ΔQ / %ΔP.',f'Ed = {ep:.3f} (signed); |Ed| = {abs(ep):.3f}.','Compare magnitude with 1 to classify elasticity.','State that this uses initial-base percentage changes, not midpoint method.'])
        if n==5:
            p=10+v*2; q=3+v
            return (f'A competitive firm sells {q} units at a constant market price of ₹{p} per unit. Calculate total revenue and average revenue.',
               ['TR = price × quantity.',f'TR = ₹{p} × {q} = ₹{p*q}.','AR = total revenue / quantity.',f'AR = {p*q}/{q} = ₹{p} per unit.','For a price-taking firm, AR equals market price in this simple example.','Check that quantities and currency units are consistent.'])
        income=1000+v*200; save=200+v*25; con=income-save
        return (f'Income is ₹{income}; saving is ₹{save}. Find consumption and APC (average propensity to consume).',
               ['Income = consumption + saving.',f'Consumption = {income} − {save} = ₹{con}.','APC = consumption / income.',f'APC = {con}/{income} = {con/income:.2f}.','APC is a ratio, not the marginal propensity.','Check that consumption plus saving equals income.'])
    return None

# Each set varies its focus, context, diagrams and numericals.
CONTEXT={
 'Accountancy':['a village cooperative','a family-run bookstore','a student stationery partnership','a local handicraft company','a community printing enterprise'],
 'Business_Studies':['a regional stationery business','a community handicraft cooperative','a school supply enterprise','a small sustainable packaging company','a local book distributor'],
 'Economics':['a market for notebooks','a local transport market','a consumer goods market','a district vegetable market','a regional grain market'],
 'History':['archaeological evidence and society','imperial administration and social life','religious movements and travel accounts','colonial policies and resistance','nationalism and constitutional change'],
 'Geography':['population and settlements','resources and sustainable development','agriculture and industries','transport and trade','human environment and regional planning'],
 'Political_Science':['the Cold War and international institutions','globalisation and regional links','Indian democracy and coalitions','federalism and public participation','nation-building and international cooperation']
}

MAP_SITE_GROUPS={
 'Geography':[
 [('Kandla',22.99,70.22),('Mumbai',19.08,72.88),('Chennai',13.08,80.27),('Visakhapatnam',17.69,83.22),('Kolkata',22.57,88.36)],
 [('Delhi',28.61,77.21),('Raipur',21.25,81.63),('Hyderabad',17.39,78.49),('Bengaluru',12.97,77.59),('Jaipur',26.91,75.79)],
 [('Mumbai',19.08,72.88),('Kolkata',22.57,88.36),('Chennai',13.08,80.27),('Kochi',9.93,76.27),('New Mangalore',12.92,74.86)],
 [('Surat',21.17,72.83),('Varanasi',25.32,83.01),('Nagpur',21.15,79.09),('Patna',25.61,85.14),('Srinagar',34.08,74.87)],
 [('Guwahati',26.14,91.74),('Bhopal',23.26,77.41),('Raipur',21.25,81.63),('Lucknow',26.85,80.95),('Ahmedabad',23.02,72.57)]
 ],
 'History':[
 [('Harappa',30.63,72.87),('Mohenjo-daro',27.33,68.14),('Kalibangan',29.47,74.13),('Lothal',22.52,72.25),('Dholavira',23.89,70.21)],
 [('Sanchi',23.48,77.74),('Sarnath',25.38,83.02),('Bodh Gaya',24.70,84.99),('Nalanda',25.14,85.44),('Amaravati',16.57,80.36)],
 [('Hampi',15.34,76.46),('Agra',27.18,78.02),('Fatehpur Sikri',27.10,77.66),('Delhi',28.61,77.21),('Jaipur',26.91,75.79)],
 [('Meerut',28.98,77.71),('Delhi',28.61,77.21),('Kanpur',26.45,80.33),('Jhansi',25.45,78.57),('Lucknow',26.85,80.95)],
 [('Sabarmati',23.06,72.58),('Dandi',20.88,72.80),('Champaran',26.66,84.92),('Kheda',22.75,72.68),('Kolkata',22.57,88.36)]
 ]
}

# Plot real coastlines/political borders (Basemap built-in datasets) with correctly
# placed study locations. Map questions refer to the labels A-E, not an invented outline.
def make_marked_map(sub,st):
    sites=MAP_SITE_GROUPS[sub][st-1]
    path=MAPS/f'{sub}_{st}.png'
    if path.exists():return path
    fig,ax=plt.subplots(figsize=(7.0,5.0),dpi=135)
    m=Basemap(projection='merc',llcrnrlat=6.2,urcrnrlat=37.0,
              llcrnrlon=62,urcrnrlon=98.8,resolution='l',ax=ax)
    m.drawmapboundary(fill_color='#e8f6fd',linewidth=0.8)
    m.fillcontinents(color='#f2f4e8',lake_color='#e8f6fd')
    m.drawcoastlines(color='#667b91',linewidth=.65)
    m.drawcountries(color='#6e8396',linewidth=.65)
    m.drawparallels(range(10,40,10),labels=[True,False,False,False],linewidth=.2,color='#cad3dc',fontsize=6)
    m.drawmeridians(range(65,100,10),labels=[False,False,False,True],linewidth=.2,color='#cad3dc',fontsize=6)
    for k,(city,lat,lon) in enumerate(sites):
        x,y=m(lon,lat);ax.plot(x,y,'o',markersize=5,color='#d96e45');ax.text(x+22000,y+17000,chr(65+k),size=9,weight='bold',color='#153f79')
    ax.set_title('Map-marking practice: identify labels A–E\nRealistic coastlines; educational map, not a survey document',fontsize=9,color='#102b56')
    fig.tight_layout()
    fig.savefig(path,dpi=135,bbox_inches='tight',facecolor='white');plt.close(fig)
    return path

for k in ('History','Geography'):
    for st in range(1,6):make_marked_map(k,st)


def esc(s):return html.escape(str(s),quote=True)
def visual(kind):
    if kind in ('org','flow','ledger','demand','supply','cost','circular','money','timeline','city','map'):
        starts='<svg viewBox="0 0 660 220" width="100%" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Original labelled educational schematic"><rect x="1" y="1" width="658" height="218" rx="10" fill="#f5faff" stroke="#d5e5f3"/>'
        end='</svg>'
        if kind=='org':
            return starts+'''<rect x="233" y="25" width="194" height="42" rx="7" fill="#154b94"/><text x="330" y="51" text-anchor="middle" fill="white" font-size="16">Top management</text><path d="M330 67V91M130 91H530M130 91V115M330 91V115M530 91V115" stroke="#288e8c" stroke-width="3" fill="none"/><g fill="#cfebe7" stroke="#65aaa4"><rect x="38" y="115" width="184" height="50" rx="9"/><rect x="237" y="115" width="184" height="50" rx="9"/><rect x="436" y="115" width="184" height="50" rx="9"/></g><g font-size="14" text-anchor="middle" fill="#103d77"><text x="130" y="145">Production</text><text x="330" y="145">Marketing</text><text x="530" y="145">Finance</text></g><text x="330" y="201" font-size="13" text-anchor="middle" fill="#52617a">Illustrative reporting structure - not a universal model</text>'''+end
        if kind=='flow':
            return starts+'''<g fill="#d9effa" stroke="#79a9cc"><rect x="25" y="80" width="144" height="64" rx="10"/><rect x="258" y="80" width="144" height="64" rx="10"/><rect x="490" y="80" width="144" height="64" rx="10"/></g><g stroke="#24937c" stroke-width="4" fill="none"><path d="M169 112H250M240 104l10 8-10 8M402 112H481M471 104l10 8-10 8"/></g><g font-size="16" text-anchor="middle" fill="#164278"><text x="97" y="111">Plan</text><text x="330" y="111">Act</text><text x="562" y="111">Review</text></g><text x="329" y="185" text-anchor="middle" font-size="13" fill="#506883">Steps must be checked against evidence and revised as necessary.</text>'''+end
        if kind=='ledger':
            return starts+'''<rect x="69" y="29" width="522" height="160" rx="10" fill="#fff" stroke="#a8bed4"/><path d="M330 52V187M70 86H590" stroke="#96adc4" stroke-width="2"/><g fill="#184d88" font-size="16" font-weight="700"><text x="206" y="55" text-anchor="middle">Debit (Dr)</text><text x="461" y="55" text-anchor="middle">Credit (Cr)</text></g><g font-size="14" fill="#29455e"><text x="91" y="118">Account receiving value</text><text x="349" y="118">Account giving value</text><text x="91" y="155">Amount: ₹ ...</text><text x="349" y="155">Amount: ₹ ...</text></g><text x="330" y="208" font-size="12" text-anchor="middle" fill="#587080">For every completed journal entry, total debits equal total credits.</text>'''+end
        if kind in ('demand','supply','cost'):
            line=('<path d="M140 60L514 169"/>' if kind=='demand' else '<path d="M140 171L514 62"/>')
            label='D (downward sloping)' if kind=='demand' else 'S (upward sloping)'
            return starts+'''<path d="M101 29V187H580" stroke="#183b65" stroke-width="3" fill="none"/><path d="M95 42l6-13 6 13M567 181l13 6-13 6" stroke="#183b65" fill="none"/><g stroke="#e58a38" stroke-width="5" fill="none">'''+line+'''</g><text x="48" y="36" font-size="15" fill="#21466f">Price</text><text x="490" y="207" font-size="15" fill="#21466f">Quantity</text><text x="290" y="40" font-size="17" fill="#17588a">'''+label+'''</text><text x="290" y="210" font-size="11" fill="#60728d">Schematic with other influences held constant.</text>'''+end
        if kind=='timeline':
            return starts+'''<path d="M66 124H590" stroke="#197f92" stroke-width="4"/><path d="M575 116l15 8-15 8" stroke="#197f92" stroke-width="3" fill="none"/><g fill="#fbdc97" stroke="#c68b32" stroke-width="2"><circle cx="124" cy="124" r="11"/><circle cx="260" cy="124" r="11"/><circle cx="402" cy="124" r="11"/><circle cx="530" cy="124" r="11"/></g><g font-size="14" fill="#174b7e" text-anchor="middle"><text x="124" y="88">Context</text><text x="260" y="88">Change</text><text x="402" y="88">Response</text><text x="530" y="88">Impact</text></g><text x="330" y="185" font-size="13" fill="#566d86" text-anchor="middle">Analyse chronology; arrows show study sequence, not fixed historic dates.</text>'''+end
        if kind in ('circular','money'):
            return starts+'''<circle cx="330" cy="115" r="76" fill="#ecf7ff" stroke="#2b7298" stroke-width="3"/><g fill="#174578" font-size="16" text-anchor="middle"><text x="330" y="109">Income</text><text x="330" y="134">& spending</text></g><g fill="#d9f2e9" stroke="#6ebda9"><rect x="30" y="86" width="176" height="59" rx="10"/><rect x="454" y="86" width="176" height="59" rx="10"/></g><g fill="#24537b" font-size="15" text-anchor="middle"><text x="118" y="120">Households</text><text x="542" y="120">Firms</text></g><g stroke="#268e8d" stroke-width="3" fill="none"><path d="M206 93L253 75M245 71l8 4-3 9M407 156l47 0m-10-8l10 8-10 8"/></g><text x="330" y="211" font-size="12" fill="#536984" text-anchor="middle">Simplified circular-flow teaching diagram</text>'''+end
        if kind=='city':
            return starts+'''<g fill="#d9eef8" stroke="#4a98b9" stroke-width="3"><rect x="55" y="49" width="143" height="90"/><rect x="254" y="49" width="143" height="90"/><rect x="454" y="49" width="143" height="90"/></g><path d="M0 170H660" stroke="#c7bcae" stroke-width="20"/><g font-size="14" fill="#245375" text-anchor="middle"><text x="127" y="98">Homes</text><text x="326" y="98">Services</text><text x="525" y="98">Jobs</text></g><text x="327" y="205" font-size="12" fill="#526d88" text-anchor="middle">Urban services require integrated transport and water planning.</text>'''+end
    return ''

CSS='''@page{size:A4;margin:15mm 16mm 16mm 16mm;@bottom-center{content:"SAURABH’S VISION  |  Independent Class 12 Exam Practice   •   Page " counter(page);font:8px "Noto Sans",sans-serif;color:#687996}}
*{box-sizing:border-box}body{font-family:"Noto Sans","Noto Sans Devanagari",sans-serif;font-size:10.1px;line-height:1.55;color:#19304e}
h1,h2,h3{color:#0c336c}h1{font-size:27px;margin:4px 0 10px}h2{font-size:16.2px;margin:26px 0 10px;padding:8px 0 6px;border-bottom:2px solid #45af9c;break-after:avoid}h3{font-size:12px;color:#24558a;margin:16px 0 8px;break-after:avoid}
small,.tiny{font-size:9px}.tag{font-size:11px;letter-spacing:1.3px;text-transform:uppercase;color:#e3f5ff}.hero{background:#0b316a;color:white;padding:26px;border-radius:14px;margin:9px 0 23px}.hero h1{color:white}.hero p{color:#d3e8fd}.sub{font-size:13px;font-weight:700;color:#d0f9ea}.banner{border:1px solid #d4dfed;background:#f4faff;padding:10px 14px;border-radius:7px;line-height:1.6}.warn{background:#fff5dc;border-left:4px solid #e6b34c;padding:11px 15px;margin:10px 0;border-radius:4px}.stats{display:flex;gap:10px;flex-wrap:wrap}.pill{display:inline-block;background:#e6f7f2;color:#0c755e;padding:5px 11px;margin-right:8px;border-radius:15px;font-size:11px;font-weight:700}
.question{page-break-inside:avoid;border-left:3px solid #a6c6e8;padding:8px 11px;margin:10px 0;background:#f9fbff}.qline{font-weight:700;color:#123969;font-size:10.7px}.marks{float:right;color:#0f837a;font-weight:700}.options{font-size:9.9px;margin-top:7px;color:#334e6b}.answer{page-break-inside:avoid;margin:11px 0;padding:9px 12px;background:#f5fcf9;border-left:3px solid #35a38b}.answer .qline{color:#117b62}.steps{padding-left:19px;margin:6px 0 3px}.steps li{padding:1px 0}.answer small{color:#586f7f}.figure{page-break-inside:avoid;border:1px solid #dae6f1;border-radius:8px;padding:5px 12px;margin:9px 0 13px}.figure svg{height:auto;max-height:200px}.figure img{width:100%;max-width:630px;max-height:355px;object-fit:contain}.figlabel{color:#466381;font-size:9px;text-align:center}.section-intro{font-weight:700;color:#526888}.pagebreak{break-before:page}.rubric{background:#eaf6ff;border-radius:7px;padding:9px 11px;margin:11px 0}.code{font-family:"DejaVu Sans Mono",monospace}.qc{font-weight:700;color:#108269}
'''

def q_obj(sub,st):
    arr=OBJECTIVES[sub];rng=random.Random(f'objective-{sub}-{st}')
    choice=rng.sample(range(len(arr)),5)
    qs=[]
    for j,ix in enumerate(choice):
        prompt,options,index,why=arr[ix]
        correct=options[index];shuffled=list(options);rng.shuffle(shuffled);letter='ABCD'[shuffled.index(correct)]
        qs.append(dict(prompt=prompt,marks=1,options=shuffled,
               steps=[f'Correct option: ({letter}) {correct}.',f'Reason: {why}','Check the definition rather than guessing from the wording.']))
    for j in range(5):
        c=CONCEPTS[sub][(st*3+j*7)%len(CONCEPTS[sub])]
        qs.append(dict(prompt=f'In one sentence, define {c[0].lower()}.',marks=1,steps=[c[1]]))
    return qs

def concept_question(sub,st,mark,seed,k):
    bank=CONCEPTS[sub]
    c=bank[(st*3 + seed*7 + k*11) % len(bank)]
    context=CONTEXT[sub][st-1]
    if mark==2: txt=f'State two key ideas about {c[0].lower()} with reference to {context}.'
    elif mark==3: txt=f'Explain three practical aspects of {c[0].lower()}; add a relevant example.'
    elif mark==4: txt=f'Discuss four points about {c[0].lower()} and explain why they matter.'
    elif mark==5: txt=f'Explain five well-organised points on {c[0].lower()}, relating them to {context} where appropriate.'
    elif mark==6: txt=f'Analyse {c[0].lower()} in six connected steps, including an application and a limitation.'
    elif mark==10: txt=f'Prepare a detailed ten-mark answer about {c[0].lower()} with explanation, illustrative working and a verification checklist.'
    else: raise ValueError(mark)
    steps=[c[1]] + c[2]
    # Each answer earns marks for concrete points and reasoning, not just a generic definition.
    if mark<5:ans=steps[:mark]
    elif mark==5:ans=steps[1:6]
    elif mark==6:ans=steps[1:7]
    else:ans=steps+['Apply the method to numerical data or a simple case when relevant.',
                   'Reconcile totals and write units/currency for figures.',
                   'State the limitation or assumption in the conclusion.']
    return dict(prompt=txt,marks=mark,steps=ans,diagram=c[3],topic=c[0])

def question_sections(sub,st):
    mark=SUBJECTS[sub]['marks'];qs=q_obj(sub,st);sections=[('Q1(A) & Q1(B) • Objective / Very Short',qs)]
    seed=0
    def add(title,n,marks,num=False):
        nonlocal seed
        a=[]
        for k in range(n):
            item=concept_question(sub,st,marks,seed,k)
            if num and sub in ('Economics','Accountancy') and (k%2==0 or marks>=6):
                data=numerical(sub,st,seed*5+k,marks)
                if data:
                    item['prompt'],item['steps']=data;item['diagram']='ledger' if sub=='Accountancy' else 'demand'
            if marks>=5 and sub not in ('Economics','Accountancy'):
                item['prompt']+=' Include a short concluding sentence.'
            a.append(item)
        sections.append((title,a));seed+=1
    if sub in ('Business_Studies','Economics'):
        add('Q2–Q6 • Short Answer I • 5 × 2 = 10',5,2,True)
        add('Q7–Q10 • Short Answer II • 4 × 3 = 12',4,3,True)
        add('Q11–Q16 • Long Answer • 6 × 4 = 24',6,4,True)
        add('Q17–Q20 • Very Long Answer • 4 × 6 = 24',4,6,True)
    elif sub=='Geography':
        add('Q2–Q6 • Short Answer I • 5 × 2 = 10',5,2)
        add('Q7–Q11 • Short Answer II • 5 × 3 = 15',5,3)
        add('Q12–Q16 • Long Answer • 5 × 5 = 25',5,5)
        places=MAP_SITE_GROUPS[sub][st-1]
        a=[dict(prompt='On the map supplied, identify places marked A–E (1 mark each).',marks=5,
                steps=[f'{chr(65+i)} = {name} (approx. {lat:.2f}°N, {lon:.2f}°E).' for i,(name,lat,lon) in enumerate(places)],diagram='map',map_image=str(make_marked_map(sub,st))),
           dict(prompt='Using the same labelled map, write the approximate latitude or geographic location of each place A–E (1 mark each).',marks=5,
                steps=[f'{chr(65+i)} ({name}): approximately {lat:.1f}°N, {lon:.1f}°E.' for i,(name,lat,lon) in enumerate(places)],diagram='map',map_image=str(make_marked_map(sub,st)))]
        sections.append(('Q17–Q18 • Map Work • 2 × 5 = 10',a))
    elif sub=='History':
        add('Q2–Q7 • Short Answer I • 6 × 2 = 12',6,2)
        add('Q8–Q12 • Short Answer II • 5 × 3 = 15',5,3)
        add('Q13–Q16 • Long Answer • 4 × 5 = 20',4,5)
        sites=MAP_SITE_GROUPS[sub][st-1]
        sections.append(('Q17 • Historical Map Work • 5 × 1 = 5',[dict(prompt='Identify the five historical places marked A–E on the supplied location map.',marks=5,
            steps=[f'{chr(65+i)} = {name} (approx. {lat:.1f}°N, {lon:.1f}°E).' for i,(name,lat,lon) in enumerate(sites)],diagram='map',map_image=str(make_marked_map(sub,st)))]))
        add('Q18–Q20 • Very Long Answer • 3 × 6 = 18',3,6)
    elif sub=='Political_Science':
        add('Q2–Q7 • Short Answer I • 6 × 2 = 12',6,2)
        add('Q8–Q12 • Short Answer II • 5 × 3 = 15',5,3)
        add('Q13–Q17 • Long Answer I • 5 × 5 = 25',5,5)
        add('Q18–Q20 • Very Long Answer II • 3 × 6 = 18',3,6)
    elif sub=='Accountancy':
        add('Short Answers • 4 × 3 = 12',4,3,True)
        add('Applied Accounts • 5 × 4 = 20',5,4,True)
        add('Working Notes / Journal Entries • 3 × 6 = 18',3,6,True)
        add('Section B: Financial Analysis / Computerised Accounts • 2 × 10 = 20',2,10,True)
    total=sum(q['marks'] for _,items in sections for q in items)
    if total!=mark:raise AssertionError((sub,st,total,mark))
    return sections


def question_html(q,no):
    text=f'<div class="question"><div class="qline">{no}. {esc(q["prompt"])}<span class="marks">[{q["marks"]} mark{"s" if q["marks"]!=1 else ""}]</span></div>'
    if 'options' in q:
        text+='<div class="options">'+' &nbsp;&nbsp; '.join(f'({"ABCD"[k]}) {esc(a)}' for k,a in enumerate(q['options']))+'</div>'
    if q.get('map_image'):text+=f'<div class="figure"><img src="file://{q["map_image"]}" alt="Map with five accurately positioned marked locations"/><div class="figlabel">Study map: A–E marks, coordinates approximate. Use a proper outline map for final examination practice.</div></div>'
    return text+'</div>'

def solution_html(q,no):
    steps=''.join(f'<li>{esc(s)}</li>' for s in q['steps'])
    text=f'<div class="answer"><div class="qline">{no}. Worked answer <span class="marks">[{q["marks"]} marks]</span></div><ol class="steps">{steps}</ol>'
    if q.get('map_image'):
        text+=f'<div class="figure"><img src="file://{q["map_image"]}" alt="Solved, labelled physical map"/><div class="figlabel">Study location diagram; use an approved examination map when required.</div></div>'
    elif q.get('show_diagram') and q.get('diagram'):
        text+='<div class="figure">'+visual(q['diagram'])+'<div class="figlabel">Original conceptual diagram for understanding; not a replacement for a formal numerical working.</div></div>'
    if q['marks']>=4:text+='<div class="qc">Exam tip: label units, give only evidence-supported statements and check each marked point.</div>'
    return text+'</div>'


def render_one(sub,st):
    specs=SUBJECTS[sub];parts=question_sections(sub,st)
    paper_path=PDFROOT/f'Class_12/12_{sub}/Practice_Set_{st:02d}.pdf'
    paper_path.parent.mkdir(parents=True,exist_ok=True)
    sections_html='';solutions_html='';num=0;manifest=[]
    board_q=1;schematic_used=0
    for group_idx,(title,items) in enumerate(parts):
        sections_html+=f'<h2>{esc(title)}</h2>'
        solutions_html+=f'<h2>{esc(title)} - Worked Solutions</h2>'
        for idx,q in enumerate(items):
            num+=1
            if group_idx==0:
                number=f'Q1({"A" if idx<5 else "B"})-{idx+1 if idx<5 else idx-4}'
            else:
                number=f'Q{board_q}'
                board_q+=1
            # Avoid repeated generic illustrations. Keep meaningful original maps;
            # use up to two supporting labelled concept diagrams per set.
            if q.get('diagram') not in (None,'flow','map') and q['marks']>=5 and schematic_used<2:
                q['show_diagram']=True;schematic_used+=1
            sections_html+=question_html(q,number)
            solutions_html+=solution_html(q,number)
            manifest.append({'number':number,'marks':q['marks'],'prompt':q['prompt'],'solution_steps':len(q['steps']),'diagram':q.get('diagram')})
    total=sum(x['marks'] for x in manifest)
    topic=CONTEXT[sub][st-1]
    body=f'''<!doctype html><html lang="en"><head><meta charset="utf-8"/><style>{CSS}</style></head><body>
    <div class="hero"><div class="tag">SAURABH'S VISION • VISION FLASH • CGBSE PATTERN PRACTICE</div>
    <h1>Class 12 • {esc(sub.replace("_"," "))}</h1><div class="sub">Independent Practice Set {st} / 5</div>
    <p>Time: 3 hours  •  Maximum marks: {total}  •  Subject code: {specs['code']}</p>
    <div style="color:#e4faff">Theme: {esc(topic)} &nbsp;&bull;&nbsp; Answer key and worked steps begin after the questions.</div></div>
    <div class="warn">INDEPENDENT REVISION PAPER — Not the official CGBSE board paper or certified answer key. Pattern and marks are based on the uploaded 2025–26 model paper. Attempt first without consulting the worked section; ask a teacher to review descriptive answers.</div>
    <div class="stats"><span class="pill">{len(manifest)} scored questions</span><span class="pill">{total} total marks</span><span class="pill">5 different sets per subject</span><span class="pill">Worked steps + visual aids</span></div>
    <h2>Instructions</h2><div class="banner">Attempt every compulsory question. Write the relevant answer points according to allocated marks. For numericals, show formula, substitution, units and check. For map questions, use the supplied map or the board-required blank outline, as directed. Do not copy worked solutions until after attempting the paper.</div>
    {sections_html}
    <div class="pagebreak"><h1>STEP-BY-STEP ANSWERS</h1><div class="banner">First identify the concept, then write the reasoning and finally verify the result. For objective questions, the correct choice and its reason are given. Descriptive answers are study guides, not the only valid wording.</div></div>
    {solutions_html}
    <h2>Student self-check checklist</h2><ol><li>Is every question number answered?</li><li>Are formula, substitutions and units shown for numerical questions?</li><li>Do diagrams have a title and clearly readable labels?</li><li>Is the response length appropriate to the marks?</li><li>Do all section totals add to {total} marks?</li><li>Have you asked your teacher to review any unclear answer?</li></ol>
    </body></html>'''
    HTML(string=body,base_url='file:///mnt/data/vision_class12_expansion/').write_pdf(str(paper_path))
    with fitz.open(paper_path) as d:
        pages=len(d)
        if pages<4:raise AssertionError('Unexpectedly short paper '+str(paper_path))
        texts=' '.join(p.get_text() for p in d)
        for s in ['STEP-BY-STEP ANSWERS',f'Maximum marks: {total}']:
            if s not in texts:raise AssertionError('Missing text '+s)
    return {'subject':sub,'set':st,'path':str(paper_path),'questions':len(manifest),'marks':total,
            'pages':pages,'size_bytes':paper_path.stat().st_size,'has_map':any(x.get('diagram')=='map' for x in manifest),
            'has_schematic':any(x.get('diagram') and x['marks']>=5 for x in manifest),
            'fingerprint':hashlib.sha256(paper_path.read_bytes()).hexdigest()[:16]}


def build():
    results=[]
    for sub in SUBJECTS:
        for st in range(1,6):
            info=render_one(sub,st);results.append(info)
            print(f"OK {sub} {st}: {info['marks']} marks, {info['questions']} questions, {info['pages']} PDF pages",flush=True)
    srcdir=PDFROOT/'Class_12';sha={}
    for sub,spec in SUBJECTS.items():
        orig=Path(os.environ.get('VISION_CLASS12_SOURCES','/mnt/data'))/spec['source']
        copied=srcdir/f'12_{sub}/Original_2025-26.pdf';copied.write_bytes(orig.read_bytes());sha[sub]=hashlib.sha256(orig.read_bytes()).hexdigest()
        if copied.read_bytes()!=orig.read_bytes():raise AssertionError('Source altered')
    out=ROOT/'Class12_6_New_Subjects_36_PDFs.zip'
    with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for path in sorted(PDFROOT.rglob('*.pdf')):z.write(path,path.relative_to(PDFROOT).as_posix())
    with zipfile.ZipFile(out) as z:
        bad=z.testzip();names=z.namelist()
        if bad:raise AssertionError('Bad ZIP '+bad)
        assert len(names)==36 and len(set(names))==36
        for n in names:
            assert z.read(n)[:5]==b'%PDF-'
    qa={'title':'Class 12 expansion: Six more subjects, original PDFs unchanged, five independent full-mark guided sets each.',
        'original_papers':sha,'sets':results,'counts':{'originals':6,'practice':30,'total':36},
        'archive':str(out),'zip_sha256':hashlib.sha256(out.read_bytes()).hexdigest(),
        'review':'Independent teacher review is recommended. Real Android and GitHub Pages tests are separate.'}
    qa_path=ROOT/'Class12_6_Subjects_QA.json'
    qa_path.write_text(json.dumps(qa,ensure_ascii=False,indent=2),encoding='utf8')
    print('READY',out,'ZIP bytes',out.stat().st_size,flush=True)
    return out,qa

if __name__=='__main__':build()