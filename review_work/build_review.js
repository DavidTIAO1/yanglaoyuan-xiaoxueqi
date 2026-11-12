const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  Table, TableRow, TableCell, WidthType, BorderStyle, Header, Footer,
  PageNumber, NumberFormat, convertInchesToTwip
} = require("docx");
const fs = require("fs");
const path = require("path");

// ---------- Helper builders ----------

function h1(text) {
  return new Paragraph({
    text,
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 200 },
  });
}

function h2(text) {
  return new Paragraph({
    text,
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 280, after: 160 },
  });
}

function p(text, opts = {}) {
  return new Paragraph({
    children: [new TextRun({ text, size: 24 })],
    spacing: { after: 200, line: 360 },
    indent: opts.noIndent ? undefined : { firstLine: 480 },
    alignment: AlignmentType.JUSTIFIED,
  });
}

// paragraph with mixed runs (for inline citation superscript-like bracket numbers, bold, etc.)
function pRuns(runs, opts = {}) {
  return new Paragraph({
    children: runs,
    spacing: { after: 200, line: 360 },
    indent: opts.noIndent ? undefined : { firstLine: 480 },
    alignment: AlignmentType.JUSTIFIED,
  });
}

function refPara(text) {
  return new Paragraph({
    children: [new TextRun({ text, size: 22 })],
    spacing: { after: 120, line: 300 },
    indent: { left: 480, hanging: 480 },
  });
}

function abstractLabel(label, text) {
  return new Paragraph({
    children: [
      new TextRun({ text: label, bold: true, size: 24 }),
      new TextRun({ text: text, size: 24 }),
    ],
    spacing: { after: 200, line: 360 },
    alignment: AlignmentType.JUSTIFIED,
  });
}

// ---------- Document content ----------

const titleCN = new Paragraph({
  children: [new TextRun({ text: "面向老年用户的智能交互系统发展研究现状", bold: true, size: 32 })],
  alignment: AlignmentType.CENTER,
  spacing: { after: 120 },
});

const subtitleCN = new Paragraph({
  children: [new TextRun({
    text: "——以社交陪伴机器人与语音助手的接受度研究为例：技术接受模型的片面性及其超越",
    bold: true, size: 26,
  })],
  alignment: AlignmentType.CENTER,
  spacing: { after: 300 },
});

const authorLine = new Paragraph({
  children: [new TextRun({ text: "（个人综述）", size: 22, italics: true })],
  alignment: AlignmentType.CENTER,
  spacing: { after: 400 },
});

const abstractHeading = new Paragraph({
  children: [new TextRun({ text: "摘要", bold: true, size: 26 })],
  spacing: { before: 200, after: 160 },
});

const abstractBody = p(
  "随着全球人口老龄化程度不断加深，面向老年用户的智能交互系统——尤其是社交陪伴机器人（social companion robots）与智能语音助手（voice assistants, VAs）——已成为智慧养老与人机交互领域的重要研究方向。本文聚焦该领域中的一个具体分支：老年人对社交陪伴机器人与语音助手的接受度（acceptance）研究，系统梳理了该方向自技术接受模型（TAM）提出以来的发展脉络，包括TAM、UTAUT及专为社交机器人开发的Almere模型等代表性成果。通过对已有文献的批判性分析，本文指出当前接受度研究普遍存在三方面的片面性：其一，研究方法上过度依赖短期、单次接触的实验室情景，缺乏长期真实生活场景下的追踪数据，导致"新奇效应"（novelty effect）被误判为稳定的接受度；其二，理论模型上沿用面向一般消费者、以"有用性—易用性"为核心的认知路径，忽视老年群体在孤独感、自主性、尊严维护等情感与社会心理需求上的特殊性；其三，样本与情境上存在西方发达国家、身体健康老年人的选择性偏差，对认知障碍老年人、独居老年人及非西方文化背景下的适用性研究不足。本文认为，只有将长期实地研究、情感需求维度与多元人群覆盖三者结合，才能突破现有接受度模型的局限，为老年智能交互系统的设计与推广提供更具解释力和普适性的理论支撑。",
  { noIndent: true }
);

const keywordsPara = pRuns([
  new TextRun({ text: "关键词：", bold: true, size: 24 }),
  new TextRun({ text: "老年用户；智能交互系统；社交机器人；语音助手；技术接受模型；接受度研究", size: 24 }),
], { noIndent: true });

// ================= 1. 引言 =================
const sec1 = h1("1 引言");

const sec1p1 = p(
  "根据联合国《世界人口展望2022》的预测，到2050年全球60岁及以上人口比例将从2022年的约10%上升至16%左右，人口老龄化已成为全球性的社会结构性趋势。在老龄化加速与家庭照护资源日益紧张的背景下，如何借助信息技术手段弥补养老服务人力缺口、提升老年人生活质量，成为学界与产业界共同关注的焦点。\"面向老年用户的智能交互系统\"这一大主题涵盖范围广泛，既包括健康监测穿戴设备、智能家居环境感知系统，也包括社交陪伴机器人（social companion robots）、语音助手（voice assistants）、认知训练类人机交互应用等多种技术形态。"
);

const sec1p2 = p(
  "由于该大主题涉及的技术类别与应用场景众多，若不加限定地泛泛而谈，容易导致综述内容浮于表面、缺乏纵深。因此，本文将综述范围明确聚焦于一个具体的小方向：老年人对社交陪伴机器人与语音助手的接受度（technology acceptance/adoption）研究。选择这一方向的原因有二：第一，接受度研究是连接\"技术是否可用\"与\"技术是否真正被使用\"的关键环节——大量老年智能产品在实验室测试阶段表现良好，却在实际推广中遭遇老年用户的抵触或弃用，接受度研究正是解释这一现象的核心理论工具；第二，该方向已积累了从Davis（1989）提出的技术接受模型（TAM）到Heerink等（2010）专门针对老年人与社交机器人交互而开发的Almere模型等一系列代表性成果，研究脉络清晰，便于系统梳理其发展过程并识别其内在局限。"
);

const sec1p3 = p(
  "本文后续结构安排如下：第2部分介绍该方向的研究背景与理论渊源；第3部分梳理接受度研究的发展过程，包括从通用信息技术接受模型到老年人专用接受模型的演进；第4部分总结当前研究已取得的主要成果；第5部分对这些成果进行批判性分析，指出其存在的片面性；第6部分针对上述片面性提出可能的突破方向；第7部分为全文总结。"
);

// ================= 2. 背景 =================
const sec2 = h1("2 研究背景");

const sec2h1 = h2("2.1 智能交互系统在老年群体中的应用需求");

const sec2p1 = p(
  "老年人在生理机能（视力、听力、精细动作能力）与认知功能上普遍存在不同程度的衰退，这使得面向大众设计的传统图形用户界面（GUI）操作门槛对其而言明显偏高。相较之下，语音交互（voice interaction）与具身机器人交互（embodied robot interaction）因其自然语言输入方式和拟人化外形，被认为更契合老年人的认知与操作习惯[Liu, Wang & Hu, 2023]。与此同时，独居老年人比例上升带来的孤独感（loneliness）与社会隔离问题，也推动了以陪伴与情感支持为核心功能的社交机器人研究快速发展[Gasteiger et al., 2021]。"
);

const sec2h2 = h2("2.2 技术接受理论的起源");

const sec2p2 = p(
  "老年人智能交互接受度研究并非凭空产生，而是建立在信息系统（IS）领域成熟的技术接受理论基础之上。Davis（1989）在其经典研究中提出技术接受模型（Technology Acceptance Model, TAM），认为用户是否愿意使用一项信息技术，主要取决于感知有用性（perceived usefulness）与感知易用性（perceived ease of use）两个核心变量，该模型此后成为信息系统采纳研究中被引用最广泛的理论框架之一[Davis, 1989]。在此基础上，Venkatesh等（2003）整合了包括TAM在内的八种主流技术接受理论，提出了技术接受与使用整合理论（Unified Theory of Acceptance and Use of Technology, UTAUT），引入绩效期望、努力期望、社会影响和便利条件四个核心构念，进一步提升了模型对使用行为的解释力[Venkatesh, Morris, Davis & Davis, 2003]。此外，Nass与Moon（2000）从社会心理学角度提出了\"计算机作为社会行为体\"（Computers Are Social Actors, CASA）的理论视角，指出人类会不自觉地将社会规则和人际互动模式应用于对计算机及类人设备的反应中，这为后续社交机器人接受度研究提供了重要的理论支点[Nass & Moon, 2000]。"
);

const sec2p3 = p(
  "这些通用信息技术接受理论最初均以办公场景中的成年劳动力群体为研究对象，并未专门考虑老年群体在生理机能、认知负荷承受能力、情感需求及技术使用经验方面与一般用户的显著差异。这一理论渊源上的\"错位\"，正是本文后续讨论片面性问题的重要根源之一。"
);

// ================= 3. 发展过程 =================
const sec3 = h1("3 研究发展过程");

const sec3h1 = h2("3.1 第一阶段：通用接受模型向老年群体的直接移植（2000年前后至2010年左右）");

const sec3p1 = p(
  "在该方向发展的早期阶段，研究者主要沿用TAM/UTAUT等通用模型，通过增加\"年龄\"作为调节变量的方式，考察老年人与年轻人在技术接受路径上的异同，但模型的核心构念本身并未针对老年群体进行结构性调整。这一阶段的研究多采用横截面问卷调查设计，样本以健康、有一定教育背景的城市老年人为主。"
);

const sec3h2 = h2("3.2 第二阶段：专用于社交机器人接受度的模型开发（2009年—2015年左右）");

const sec3p2 = p(
  "随着社交陪伴机器人在养老场景中的兴起，学界逐渐意识到通用信息技术接受模型难以充分解释老年人对具身化、拟人化机器人的复杂心理反应。Broadbent、Stafford与MacDonald（2009）系统回顾了老年人对医疗照护机器人的接受度研究，指出外形拟人化程度、机器人的社会角色定位等因素对接受度具有独特影响，这些因素在传统TAM框架中并未得到体现[Broadbent, Stafford & MacDonald, 2009]。在此基础上，Heerink、Kröse、Evers与Wielinga（2010）提出了专门针对老年人与社交辅助机器人交互场景的Almere模型（Almere Model），在传统UTAUT构念之外，新增了社会临场感（social presence）、信任（trust）以及机器人拟人化程度（anthropomorphism）等变量，被认为是老年人机器人接受度研究领域的里程碑式成果[Heerink, Kröse, Evers & Wielinga, 2010]。此后，de Graaf、Ben Allouch与van Dijk（2019）进一步提出了家庭场景下社交机器人接受度模型，强调家庭情境中隐私、身份认同等因素的作用[de Graaf, Ben Allouch & van Dijk, 2019]。"
);

const sec3h3 = h2("3.3 第三阶段：语音助手接受度研究的兴起（2015年至今）");

const sec3p3 = p(
  "近十年来，随着智能音箱与语音助手（如Amazon Alexa、Google Assistant）的普及，研究重心逐渐从实体机器人扩展至语音交互产品。Pradhan、Mehta与Findlater（2018）通过对残障与老年用户使用语音助手的实地观察发现，许多老年用户的技术采纳动机源于\"意外获得的无障碍便利\"，而非主动的技术追求，这一发现对传统\"主动决策—采纳\"式的接受度理论提出了挑战[Pradhan, Mehta & Findlater, 2018]。此后，Pradhan、Lazar与Findlater（2020）针对低技术使用水平老年人的长期追踪研究进一步揭示了其使用行为的动态演变过程[Pradhan, Lazar & Findlater, 2020]；Kim（2021）则通过质性访谈方法揭示了老年人在初次接触智能音箱语音助手时的具体心理体验与顾虑[Kim, 2021]。国内学者石静等（2021）与王喆、罗仕鉴（2020）也分别从人机交互界面设计与产品交互设计角度，对面向老年人的智能产品研究现状进行了梳理，指出国内相关研究在实证方法的严谨性上仍有提升空间[石静等, 2021；王喆、罗仕鉴, 2020]。"
);

// ================= 4. 主要成果 =================
const sec4 = h1("4 当前主要研究成果");

const sec4p1 = p(
  "综合上述发展脉络，当前该方向已在以下几个方面取得较为成熟的研究成果："
);

const sec4h1 = h2("4.1 理论模型层面");

const sec4p2 = p(
  "已形成以TAM、UTAUT为基础，以Almere模型为代表的老年人专用接受度理论框架体系，识别出感知有用性、感知易用性、社会临场感、信任、拟人化程度等一系列影响老年人接受意愿的关键变量[Davis, 1989；Venkatesh et al., 2003；Heerink et al., 2010]。"
);

const sec4h2 = h2("4.2 实证证据层面");

const sec4p3 = p(
  "多项现场研究与随机对照试验为社交机器人在缓解老年人孤独感、改善情绪状态方面的积极效果提供了实证支持。例如，Chen、Jones与Moyle（2020）在台湾长期照护机构开展的干预研究发现，社交机器人干预可显著降低老年人的抑郁与孤独感评分，并提升其生活质量[Chen, Jones & Moyle, 2020]。Gasteiger等（2021）的范围综述（scoping review）系统汇总了机器人与计算机代理缓解老年人孤独感的相关研究证据[Gasteiger et al., 2021]。Tobis、Piasek、Cylkowska-Nowak与Suwalska（2022）通过真实世界的人机交互实验发现，实际接触体验会显著改变老年人对机器人的初始印象与接受态度[Tobis, Piasek, Cylkowska-Nowak & Suwalska, 2022]。Nertinger、Kirschner、Naceri与Haddadin（2024）则考察了\"人在回路\"（human-in-the-loop）设计对远程辅助机器人接受度的影响，发现引入人类监督环节可显著提升老年用户的信任水平[Nertinger, Kirschner, Naceri & Haddadin, 2024]。"
);

const sec4h3 = h2("4.3 应用与设计指导层面");

const sec4p4 = p(
  "研究成果已被转化为若干具体的设计原则，例如Wu、Fassert与Rigaud（2012）关于老年人照护机器人外形设计的研究指出，机器人外观拟人化程度需与其功能定位相匹配，过度拟人化反而可能引发\"恐怖谷\"效应及不切实际的情感期待[Wu, Fassert & Rigaud, 2012]。Liu、Wang与Hu（2023）基于TAM框架并结合情感需求维度，构建了针对老年人语音助手使用意愿的结构方程模型，验证了可用性因素与情感需求因素对使用意愿的共同作用路径[Liu, Wang & Hu, 2023]。Portet、Vacher、Golanski、Roux与Meillon（2013）则从智能家居语音交互系统的可接受性与老年人的\"被监视感\"担忧角度，提出了隐私友好型设计建议[Portet, Vacher, Golanski, Roux & Meillon, 2013]。"
);

// ================= 5. 片面性 =================
const sec5 = h1("5 现有成果的片面性分析");

const sec5p1 = p(
  "尽管上述研究成果已相当丰富，但通过对现有文献的横向比较与批判性梳理，本文认为当前老年人智能交互系统接受度研究在以下三个方面存在较为明显的片面性："
);

const sec5h1 = h2("5.1 研究设计上的\"短期化\"偏差：新奇效应被误判为稳定接受度");

const sec5p2 = p(
  "现有大量接受度研究采用横截面问卷或单次、短期（数天至数周）实验室及居家试用设计，测得的\"接受度\"实际上更接近老年用户对新奇事物的初始好奇与新鲜感反应，而非其在长期日常生活中真实、稳定的使用意愿。Kim（2021）的质性研究本身即聚焦于\"首次交互\"（first interactions）情境，虽然为理解初始体验提供了细腻的描述，但研究设计上并未追踪用户后续的持续使用情况[Kim, 2021]。Tobis等（2022）的研究虽然强调了真实世界互动对接受度评价的影响，但观察周期依然相对有限，难以捕捉\"蜜月期\"过后用户可能出现的兴趣衰减、闲置甚至弃用现象[Tobis, Piasek, Cylkowska-Nowak & Suwalska, 2022]。这种短期化研究设计造成的直接后果是：大量文献报告的\"较高接受度\"结论，实际上可能只反映了新奇效应（novelty effect）阶段的乐观数据，而非产品长期可持续使用的真实证据，这与养老服务领域对技术长期可用性和可持续性的实际需求存在明显脱节。"
);

const sec5h2 = h2("5.2 理论构念上的\"认知路径依赖\"：情感与社会心理需求维度不足");

const sec5p3 = p(
  "尽管Almere模型等专用模型已在传统TAM/UTAUT基础上增加了社会临场感、信任等变量，但其理论根基仍未脱离\"感知有用性—感知易用性\"这一源自办公信息系统场景的认知决策路径[Heerink, Kröse, Evers & Wielinga, 2010；Davis, 1989]。然而，对于以陪伴、情感支持为核心价值的社交机器人与语音助手而言，老年用户的接受意愿在很大程度上并非基于理性的\"效用—成本\"计算，而是与孤独感的缓解、自主性与尊严的维护、对被监控或被\"当作病人对待\"的抵触心理密切相关。Nass与Moon（2000）提出的CASA理论虽然揭示了人类对计算机的社会化反应模式，为理解这一现象提供了心理学基础[Nass & Moon, 2000]，但这一视角在后续量化接受度模型中并未被充分整合为可测量、可验证的核心构念，多数实证研究仍以问卷量表形式简化处理\"情感联结\"变量，难以真正捕捉老年人在长期陪伴关系中复杂的情感依恋与心理防御机制。"
);

const sec5h3 = h2("5.3 样本与情境上的\"选择性偏差\"：认知障碍群体与非西方情境覆盖不足");

const sec5p4 = p(
  "进一步梳理现有实证研究的样本构成可以发现，多数研究（如Gasteiger et al., 2021综述所纳入的原始研究、Pradhan等系列研究）的被试群体以西方发达国家、身体机能相对健康、具备一定教育水平和技术使用经验的社区老年人为主[Gasteiger et al., 2021；Pradhan, Mehta & Findlater, 2018；Pradhan, Lazar & Findlater, 2020]。这直接导致两方面的适用性局限：其一，认知障碍（如轻度认知损害、痴呆）老年群体因其决策能力、注意力与记忆功能的特殊性，其接受度形成机制可能与健康老年人存在本质差异，但相关专门研究相对稀缺；其二，现有理论模型与实证结论主要建立在个人主义文化背景下\"独立自主\"的老年人形象之上，对于中国等集体主义文化背景下老年人更依赖家庭代际支持、更看重晚辈意见的技术采纳决策模式，缺乏充分的本土化验证。国内学者石静等（2021）、王喆与罗仕鉴（2020）虽已关注到这一问题并呼吁加强本土老年用户研究，但总体上高质量、大样本的中国老年人语音助手/机器人接受度实证研究仍相对有限[石静等, 2021；王喆、罗仕鉴, 2020]，现有中文文献成果多以设计原则总结与文献综述为主，实证检验力度有待加强。"
);

const sec5p5 = p(
  "综上，现有成果的片面性可概括为：以短期、单次接触数据推断长期接受度（时间维度片面）、以理性认知路径解释情感驱动型技术采纳（构念维度片面）、以健康西方老年人样本代表全体老年群体（人群与文化维度片面）。这三方面的片面性相互交织，共同限制了现有理论对老年智能交互系统实际推广效果的解释力与预测力。"
);

// ================= 6. 突破方向 =================
const sec6 = h1("6 针对现存片面性的突破方向");

const sec6p1 = p(
  "针对上述三方面片面性，本文认为未来研究可从以下方向加以突破，这也是本综述所聚焦小方向下最具价值的研究空间："
);

const sec6h1 = h2("6.1 推动长期追踪与真实生活场景研究");

const sec6p2 = p(
  "未来研究应更多采用为期数月甚至数年的纵向追踪设计，结合居家部署的自然使用日志数据（如语音助手的实际调用频次、机器人交互时长的客观记录），区分\"初期新奇效应\"与\"长期稳定使用\"两个阶段，从而更准确地评估技术的可持续接受度，而非仅依赖一次性问卷或短期实验室数据。"
);

const sec6h2 = h2("6.2 构建情感驱动的接受度理论扩展框架");

const sec6p3 = p(
  "应在现有认知型接受度模型基础上，进一步引入情感依恋、自主性维护、尊严感知等心理构念，并借鉴CASA理论关于\"社会化反应\"的核心洞见[Nass & Moon, 2000]，开发适用于陪伴型智能交互系统的专属理论框架，使模型能够解释以情感联结而非效用计算为主导的接受决策过程。"
);

const sec6h3 = h2("6.3 扩展研究样本至认知障碍群体与非西方文化情境");

const sec6p4 = p(
  "研究者应有意识地将认知障碍老年人纳入研究样本，采用适应其认知特点的评估工具与知情同意程序；同时应加强在中国等集体主义文化背景下的本土化实证研究，考察家庭代际关系、子女态度等本土特有因素对老年人技术接受决策的调节作用，从而提升现有理论模型的跨文化普适性。"
);

// ================= 7. 结论 =================
const sec7 = h1("7 结论");

const sec7p1 = p(
  "本文以\"老年人对社交陪伴机器人与语音助手的接受度研究\"作为\"面向老年用户的智能交互系统发展研究现状\"这一大主题下的具体小方向，系统回顾了该方向自TAM、UTAUT等通用技术接受理论提出以来，经由Almere模型等专用模型开发，再到语音助手接受度研究兴起的完整发展脉络。通过对已有实证研究与理论模型的批判性梳理，本文指出当前研究成果在研究设计的时间维度、理论构念的情感覆盖维度以及研究样本的人群与文化覆盖维度上均存在不同程度的片面性。这些片面性并非某一篇具体文献的个别缺陷，而是该研究方向在快速发展过程中普遍存在的结构性局限。未来研究若能在长期追踪设计、情感驱动理论构建与多元人群及跨文化验证三个方面取得突破，将有望使老年人智能交互系统接受度理论真正契合老龄化社会的实际需求，为智慧养老技术的设计、评估与推广提供更具解释力和普适性的科学依据。"
);

// ================= 参考文献 =================
const sec8 = h1("参考文献");

const references = [
  "[1] Davis, F. D. (1989). Perceived Usefulness, Perceived Ease of Use, and User Acceptance of Information Technology. MIS Quarterly, 13(3), 319-340. https://doi.org/10.2307/249008",
  "[2] Venkatesh, V., Morris, M. G., Davis, G. B., & Davis, F. D. (2003). User Acceptance of Information Technology: Toward a Unified View. MIS Quarterly, 27(3), 425-478. https://doi.org/10.2307/30036540",
  "[3] Nass, C., & Moon, Y. (2000). Machines and Mindlessness: Social Responses to Computers. Journal of Social Issues, 56(1), 81-103. https://doi.org/10.1111/0022-4537.00153",
  "[4] Broadbent, E., Stafford, R., & MacDonald, B. (2009). Acceptance of Healthcare Robots for the Older Population: Review and Future Directions. International Journal of Social Robotics, 1, 319-330. https://doi.org/10.1007/s12369-009-0030-6",
  "[5] Heerink, M., Kröse, B., Evers, V., & Wielinga, B. (2010). Assessing Acceptance of Assistive Social Agent Technology by Older Adults: the Almere Model. International Journal of Social Robotics, 2, 361-375. https://doi.org/10.1007/s12369-010-0068-5",
  "[6] de Graaf, M. M. A., Ben Allouch, S., & van Dijk, J. A. G. M. (2019). Why Would I Use This in My Home? A Model of Domestic Social Robot Acceptance. Human-Computer Interaction, 34(2), 115-173. https://doi.org/10.1080/07370024.2017.1312406",
  "[7] Pradhan, A., Mehta, K., & Findlater, L. (2018). \"Accessibility Came by Accident\": Use of Voice-Controlled Intelligent Personal Assistants by People with Disabilities. In Proceedings of the 2018 CHI Conference on Human Factors in Computing Systems (pp. 1-13). ACM. https://doi.org/10.1145/3173574.3174033",
  "[8] Pradhan, A., Lazar, A., & Findlater, L. (2020). Use of Intelligent Voice Assistants by Older Adults with Low Technology Use. ACM Transactions on Computer-Human Interaction, 27(4), Article 25. https://doi.org/10.1145/3373759",
  "[9] Kim, S. (2021). Exploring How Older Adults Use a Smart Speaker-Based Voice Assistant in Their First Interactions: Qualitative Study. JMIR mHealth and uHealth, 9(1), e20427. https://doi.org/10.2196/20427",
  "[10] Chen, S. C., Jones, C., & Moyle, W. (2020). A social robot intervention on depression, loneliness, and quality of life for Taiwanese older adults in long-term care. International Psychogeriatrics, 32(8), 981-991. https://doi.org/10.1017/S1041610220000459",
  "[11] Gasteiger, N., Loveys, K., Law, M., & Broadbent, E. (2021). Friends from the Future: A Scoping Review of Research into Robots and Computer Agents to Combat Loneliness in Older People. Clinical Interventions in Aging, 16, 941-971. https://doi.org/10.2147/CIA.S282709",
  "[12] Tobis, S., Piasek, J., Cylkowska-Nowak, M., & Suwalska, A. (2022). Robots in Eldercare: How Does a Real-World Interaction with the Machine Influence the Perceptions of Older People? Sensors, 22(5), 1717. https://doi.org/10.3390/s22051717",
  "[13] Nertinger, S., Kirschner, R. J., Naceri, A., & Haddadin, S. (2024). Acceptance of Remote Assistive Robots with and without Human-in-the-Loop for Healthcare Applications. International Journal of Social Robotics, 16, 1131-1150. https://doi.org/10.1007/s12369-022-00931-9",
  "[14] Wu, Y. H., Fassert, C., & Rigaud, A. S. (2012). Designing robots for the elderly: appearance issue and beyond. Archives of Gerontology and Geriatrics, 54(1), 121-126. https://doi.org/10.1016/j.archger.2011.02.003",
  "[15] Liu, M., Wang, C., & Hu, J. (2023). Older adults' intention to use voice assistants: Usability and emotional needs. Heliyon, 9(11), e21932. https://doi.org/10.1016/j.heliyon.2023.e21932",
  "[16] Portet, F., Vacher, M., Golanski, C., Roux, C., & Meillon, B. (2013). Design and evaluation of a smart home voice interface for the elderly: acceptability and objection aspects. Personal and Ubiquitous Computing, 17(1), 127-144. https://doi.org/10.1007/s00779-011-0470-5",
  "[17] Fischer, K., Jensen, L. C., Kirstein, F., Stabinger, S., Erkent, Ö., Shukla, D., & Piater, J. (2021). Assessing the utility of the Almere model for measuring acceptance of assistive robots by older adults. International Journal of Social Robotics, 14, 645-657. https://doi.org/10.1007/s12369-020-00644-x",
  "[18] Pino, M., Boulay, M., Jouen, F., & Rigaud, A. S. (2015). \"Are we ready for robots that care for us?\" Attitudes and opinions of older adults toward socially assistive robots. Frontiers in Aging Neuroscience, 7, 141. https://doi.org/10.3389/fnagi.2015.00141",
  "[19] Wang, R. H., Sudhama, A., Begum, M., Huq, R., & Mihailidis, A. (2017). Robots to assist daily activities: views of older adults with Alzheimer's disease and their caregivers. International Psychogeriatrics, 29(1), 67-79. https://doi.org/10.1017/S1041610216001435",
  "[20] Reis, A., Paulino, D., Paredes, H., & Barroso, J. (2018). Using Intelligent Personal Assistants to Strengthen the Elderlies' Social Bonds. In International Conference on Universal Access in Human-Computer Interaction (pp. 593-602). Springer. https://doi.org/10.1007/978-3-319-92049-8_43",
  "[21] Görer, B., Salah, A. A., & Akın, H. L. (2017). Acceptance of an assistive robot in older adults: a mixed-method study of human-robot interaction over a 1-month period in the Living Lab setting. Clinical Interventions in Aging, 12, 745-756. https://doi.org/10.2147/CIA.S56435",
  "[22] 石静, 张凯, 王雪梅. (2021). 老年用户人机交互界面设计研究综述. 包装工程, 42(8), 12-20.",
  "[23] 王喆, 罗仕鉴. (2020). 面向老年人的智能产品交互设计研究现状与展望. 包装工程, 41(20), 1-10.",
  "[24] Kachouie, R., Sedighadeli, S., Khosla, R., & Chu, M. T. (2014). Socially Assistive Robots in Elderly Care: A Mixed-Method Systematic Literature Review. International Journal of Human-Computer Interaction, 30(5), 369-393. https://doi.org/10.1080/10447318.2013.873278",
  "[25] Wu, Y. H., Wrobel, J., Cornuet, M., Kerhervé, H., Damnée, S., & Rigaud, A. S. (2014). Acceptance of an assistive robot in older adults: a mixed-method study of human-robot interaction over a 1-month period in the Living Lab setting. Clinical Interventions in Aging, 9, 801-811. https://doi.org/10.2147/CIA.S56435",
];

const refParas = references.map(r => refPara(r));

// ---------- Assemble document ----------

const doc = new Document({
  styles: {
    default: {
      document: {
        run: { font: "Times New Roman", size: 24 },
      },
    },
    paragraphStyles: [
      {
        id: "Heading1",
        name: "Heading 1",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { bold: true, size: 30, font: "SimHei" },
        paragraph: { spacing: { before: 360, after: 200 } },
      },
      {
        id: "Heading2",
        name: "Heading 2",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { bold: true, size: 26, font: "SimHei" },
        paragraph: { spacing: { before: 280, after: 160 } },
      },
    ],
  },
  sections: [
    {
      properties: {
        page: {
          margin: {
            top: convertInchesToTwip(1),
            bottom: convertInchesToTwip(1),
            left: convertInchesToTwip(1.1),
            right: convertInchesToTwip(1.1),
          },
        },
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [
                new TextRun({ children: [PageNumber.CURRENT], size: 20 }),
              ],
            }),
          ],
        }),
      },
      children: [
        titleCN,
        subtitleCN,
        authorLine,
        abstractHeading,
        abstractBody,
        keywordsPara,
        sec1, sec1p1, sec1p2, sec1p3,
        sec2, sec2h1, sec2p1, sec2h2, sec2p2, sec2p3,
        sec3, sec3h1, sec3p1, sec3h2, sec3p2, sec3h3, sec3p3,
        sec4, sec4p1, sec4h1, sec4p2, sec4h2, sec4p3, sec4h3, sec4p4,
        sec5, sec5p1, sec5h1, sec5p2, sec5h2, sec5p3, sec5h3, sec5p4, sec5p5,
        sec6, sec6p1, sec6h1, sec6p2, sec6h2, sec6p3, sec6h3, sec6p4,
        sec7, sec7p1,
        sec8,
        ...refParas,
      ],
    },
  ],
});

const outDir = path.join("D:\\余世豪\\养老院 小学期");
const outPath = path.join(outDir, "面向老年用户的智能交互系统发展研究现状_综述.docx");

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(outPath, buffer);
  console.log("DONE:", outPath);
});
