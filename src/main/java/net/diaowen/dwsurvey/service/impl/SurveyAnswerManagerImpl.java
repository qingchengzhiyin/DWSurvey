package net.diaowen.dwsurvey.service.impl;

import net.diaowen.common.QuType;
import net.diaowen.common.plugs.page.Page;
import net.diaowen.common.service.BaseServiceImpl;
import net.diaowen.common.utils.RunAnswerUtil;
import net.diaowen.common.utils.excel.XLSXExportUtil;
import net.diaowen.common.utils.parsehtml.HtmlUtil;
import net.diaowen.dwsurvey.config.DWSurveyConfig;
import net.diaowen.dwsurvey.dao.SurveyAnswerDao;
import net.diaowen.dwsurvey.entity.*;
import net.diaowen.dwsurvey.service.*;
import org.aspectj.util.FileUtil;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;


/**
 * 问卷回答记录
 * @author keyuan(keyuan258@gmail.com)
 *
 * https://github.com/wkeyuan/DWSurvey
 * http://dwsurvey.net
 */
@Service
public class SurveyAnswerManagerImpl extends
		BaseServiceImpl<SurveyAnswer, String> implements SurveyAnswerManager {
	@Resource
	private SurveyAnswerManagerImpl surveyAnswerManagerImpl;

	private final SurveyAnswerDao surveyAnswerDao;
	private final QuestionManager questionManager;
	private final SurveyDirectoryManager directoryManager;
	private final AnYesnoManager anYesnoManager;
	private final AnRadioManager anRadioManager;
	private final AnFillblankManager anFillblankManager;
	private final AnEnumquManager anEnumquManager;
	private final AnDFillblankManager anDFillblankManager;
	private final AnCheckboxManager anCheckboxManager;
	private final AnAnswerManager anAnswerManager;
	private final AnScoreManager anScoreManager;
	private final AnOrderManager anOrderManager;
	private final AnUploadFileManager anUploadFileManager;

	private static final String SURVEYID = "surveyId";
	private static final String ENDANDATE = "endAnDate";
	private static final String NBSP = "&nbsp;";
	private static final String USER_ID = "userId";
	/**
	 * 日志
	 */
	private final Logger logger = Logger.getLogger(SurveyAnswerManagerImpl.class.getName());

	public SurveyAnswerManagerImpl(SurveyAnswerDao surveyAnswerDao, QuestionManager questionManager, SurveyDirectoryManager directoryManager, AnYesnoManager anYesnoManager, AnRadioManager anRadioManager, AnFillblankManager anFillblankManager, AnEnumquManager anEnumquManager, AnDFillblankManager anDFillblankManager, AnCheckboxManager anCheckboxManager, AnAnswerManager anAnswerManager, AnScoreManager anScoreManager, AnOrderManager anOrderManager, AnUploadFileManager anUploadFileManager) {
		this.surveyAnswerDao = surveyAnswerDao;
		this.questionManager = questionManager;
		this.directoryManager = directoryManager;
		this.anYesnoManager = anYesnoManager;
		this.anRadioManager = anRadioManager;
		this.anFillblankManager = anFillblankManager;
		this.anEnumquManager = anEnumquManager;
		this.anDFillblankManager = anDFillblankManager;
		this.anCheckboxManager = anCheckboxManager;
		this.anAnswerManager = anAnswerManager;
		this.anScoreManager = anScoreManager;
		this.anOrderManager = anOrderManager;
		this.anUploadFileManager = anUploadFileManager;
	}

	@Autowired



	@Override
	public void setBaseDao() {
		this.baseDao = surveyAnswerDao;
	}

	/**
	 * 保存答案
	 * @param surveyAnswer
	 * @param quMaps
	 */
	@Override
	public void saveAnswer(SurveyAnswer surveyAnswer,
			Map<String, Map<String, Object>> quMaps) {
		surveyAnswerDao.saveAnswer(surveyAnswer, quMaps);
	}

	/**
	 * 获取问卷回答细节
	 * @param answer
	 * @return
	 */
	@Override
	public List<Question> findAnswerDetail(SurveyAnswer answer) {
		String surveyAnswerId = answer.getId();
		List<Question> questions = questionManager.findDetails(answer.getSurveyId(), "2");
		for (Question question : questions) {
			getquestionAnswer(surveyAnswerId, question);
		}
		return questions;
	}


	/**
	 * 判断题目类型，并取值
	 *
	 * @param surveyAnswerId
	 * @param question
	 * @return
	 */
	public int getquestionAnswer(String surveyAnswerId, Question question) {
		int score = 0;
		String quId = question.getId();
		// 查询每一题的答案,如果是主观题，则判断是否答对
		QuType quType = question.getQuType();

		//重置导出
		question.setAnAnswer(new AnAnswer());
		question.setAnCheckboxs(new ArrayList<>());
		question.setAnDFillblanks(new ArrayList<>());
		question.setAnEnumqus(new ArrayList<>());
		question.setAnFillblank(new AnFillblank());
		question.setAnRadio(new AnRadio());
		question.setAnYesno(new AnYesno());
		question.setAnScores(new ArrayList<>());
		question.setAnOrders(new ArrayList<>());

		if (quType == QuType.YESNO) {// 是非题答案
			AnYesno anYesno = anYesnoManager.findAnswer(surveyAnswerId, quId);
			if (anYesno != null) {
				question.setAnYesno(anYesno);
			}
		} else if (quType == QuType.RADIO || quType == QuType.COMPRADIO) {// 单选题答案
			// 复合
			AnRadio anRadio = anRadioManager.findAnswer(surveyAnswerId, quId);
			if (anRadio != null) {
				question.setAnRadio(anRadio);
			}
		} else if (quType == QuType.CHECKBOX || quType == QuType.COMPCHECKBOX) {// 多选题答案
			// 复合
			List<AnCheckbox> anCheckboxs = anCheckboxManager.findAnswer(
					surveyAnswerId, quId);
			if (anCheckboxs != null) {
				question.setAnCheckboxs(anCheckboxs);
			}
		} else if (quType == QuType.FILLBLANK) {// 单项填空题答案
			AnFillblank anFillblank = anFillblankManager.findAnswer(
					surveyAnswerId, quId);
			if (anFillblank != null) {
				question.setAnFillblank(anFillblank);
			}

		} else if (quType == QuType.MULTIFILLBLANK) {// 多项填空题答案
			List<AnDFillblank> anDFillblanks = anDFillblankManager.findAnswer(
					surveyAnswerId, quId);
			if (anDFillblanks != null) {
				question.setAnDFillblanks(anDFillblanks);
			}
		} else if (quType == QuType.ANSWER) {// 问答题答案
			AnAnswer anAnswer = anAnswerManager
					.findAnswer(surveyAnswerId, quId);
			if (anAnswer != null) {
				question.setAnAnswer(anAnswer);
			}
		} else if (quType == QuType.BIGQU) {// 大题答案
			 List<Question> childQuestions=question.getQuestions();
			 for (Question childQuestion : childQuestions) {
			 	score=getquestionAnswer(surveyAnswerId, childQuestion);
			 }
		} else if (quType == QuType.ENUMQU) {
			List<AnEnumqu> anEnumqus = anEnumquManager.findAnswer(
					surveyAnswerId, quId);
			if (anEnumqus != null) {
				question.setAnEnumqus(anEnumqus);
			}
		} else if (quType == QuType.SCORE) {// 评分题
			List<AnScore> anScores = anScoreManager.findAnswer(surveyAnswerId,
					quId);
			if (anScores != null) {
				question.setAnScores(anScores);
			}
		} else if(quType == QuType.ORDERQU){
			List<AnOrder> anOrders = anOrderManager.findAnswer(surveyAnswerId,quId);
			if(anOrders!=null){
				question.setAnOrders(anOrders);
			}
		} else if (quType == QuType.UPLOADFILE) {
			List<AnUplodFile> anUplodFiles = anUploadFileManager.findAnswer(surveyAnswerId,quId);
			question.setAnUplodFiles(anUplodFiles);
		}
		return score;
	}

	/**
	 * 根据ip地址。。。。。。
	 * @param surveyDetail
	 * @param ip
	 * @return
	 */
	@Override
	public SurveyAnswer getTimeInByIp(SurveyDetail surveyDetail, String ip) {

		Calendar calendarDate = Calendar.getInstance();
		calendarDate.setTime(new Date());
		calendarDate.set(Calendar.MINUTE, calendarDate.get(Calendar.MINUTE)
				- surveyDetail.getEffectiveTime());

		return surveyAnswerDao.findFirst(ENDANDATE, true
				, Restrictions.eq(SURVEYID, surveyDetail.getDirId())
				, Restrictions.eq("ipAddr", ip)
				, Restrictions.gt(ENDANDATE, calendarDate.getTime()));

	}

	/**
	 * 根据ip获取答案数量
	 * @param surveyId
	 * @param ip
	 * @return
	 */
	@Override
	public Long getCountByIp(String surveyId, String ip) {
		String hql = "select count(*) from SurveyAnswer x where x.surveyId=?1 and x.ipAddr=?2";
        return (Long) surveyAnswerDao.findUniObjs(hql, surveyId, ip);
	}

	/**
	 * 根据ip获取回答？？
	 * @param surveyId
	 * @param ip
	 * @return
	 */
	@Override
	public List<SurveyAnswer> answersByIp(String surveyId, String ip) {
        return surveyAnswerDao.find(
				Restrictions.eq(SURVEYID, surveyId)
				, Restrictions.eq("ipAddr", ip));
	}


	/**
	 * 将问卷结果导出为xls
	 * @param surveyId
	 * @param savePath
	 * @param isExpUpQu
	 * @return
	 */
	@Override
	public String exportXLS(String surveyId, String savePath, boolean isExpUpQu) {
		String urlPath = "/webin/expfile/" + surveyId + "/";// 下载所用的地址
		String path = urlPath.replace("/", File.separator);// 文件系统路径

		savePath = savePath + path;
		File file = new File(savePath);
		if (!file.exists())
			file.mkdirs();

		String fileName = "DWSurvey-"+surveyId + ".xlsx";

		XLSXExportUtil exportUtil = new XLSXExportUtil(fileName, savePath);
		try {
			List<SurveyAnswer> answers = answerList(surveyId, 1);
			List<Question> questions = questionManager.findDetails(surveyId,"2");
			exportXLSTitle(exportUtil, questions);
			int answerListSize = answers.size();
			for (int j = 0; j < answerListSize; j++) {
				SurveyAnswer surveyAnswer = answers.get(j);
				String surveyAnswerId = surveyAnswer.getId();
				exportUtil.createRow(j+1);
				exportXLSRow(exportUtil, surveyAnswerId, questions, surveyAnswer, (j+1), savePath, isExpUpQu);
			}
			exportUtil.exportXLS();
		} catch (Exception e) {
			logger.warning(e.getMessage());
		}
		return urlPath + fileName;
	}

	/**
	 * 导出某个问卷结果为XLS格式的行
	 * @param exportUtil
	 * @param surveyAnswerId
	 * @param questions
	 * @param surveyAnswer
	 * @param orderNum
	 * @param savePath
	 * @param isExpUpQu
	 */
	private void exportXLSRow(XLSXExportUtil exportUtil,String surveyAnswerId, List<Question> questions,SurveyAnswer surveyAnswer,int orderNum, String savePath, boolean isExpUpQu) {
		new RunAnswerUtil().getQuestionMap(questions,surveyAnswerId);
		int cellIndex = 0;
		int quNum=0;
		for (Question question : questions) {
			QuType quType = question.getQuType();
			if(quType==QuType.PAGETAG || quType==QuType.PARAGRAPH){
				continue;
			}
			quNum++;
			if (quType == QuType.YESNO) {// 是非题
				String yesnoAnswer = question.getAnYesno().getYesnoAnswer();
				if("1".equals(yesnoAnswer)){
					yesnoAnswer=question.getYesnoOption().getTrueValue();
				}else if("0".equals(yesnoAnswer)){
					yesnoAnswer=question.getYesnoOption().getFalseValue();
				}else{
					yesnoAnswer="";
				}
				exportUtil.setCell(cellIndex++, yesnoAnswer);
			} else if (quType == QuType.RADIO) {// 单选题
				String quItemId = question.getAnRadio().getQuItemId();
				List<QuRadio> quRadios=question.getQuRadios();
				String answerOptionName="";
				String answerOtherText="";
				boolean isNote = false;
				for (QuRadio quRadio : quRadios) {
					if (quRadio.getIsNote() == 1) {
						isNote = true;
						break;
					}
				}

				for (QuRadio quRadio : quRadios) {
					String quRadioId=quRadio.getId();
					if(quRadioId.equals(quItemId)){
						answerOptionName=quRadio.getOptionName();
						if(quRadio.getIsNote()==1){
							answerOtherText = question.getAnRadio().getOtherText();
						}
						break;
					}
				}
				answerOptionName=HtmlUtil.removeTagFromText(answerOptionName);
				answerOptionName = answerOptionName.replace(NBSP," ");
				exportUtil.setCell(cellIndex++, answerOptionName);
				if(isNote) exportUtil.setCell(cellIndex++, answerOtherText);
			} else if (quType == QuType.CHECKBOX) {// 多选题
				List<AnCheckbox> anCheckboxs=question.getAnCheckboxs();
				List<QuCheckbox> checkboxs = question.getQuCheckboxs();
				for (QuCheckbox quCheckbox : checkboxs) {
					String quCkId=quCheckbox.getId();
					String answerOptionName="0";
					String answerOtherText="";
					for (AnCheckbox anCheckbox : anCheckboxs) {
						String anQuItemId=anCheckbox.getQuItemId();
						if(quCkId.equals(anQuItemId)){
							answerOptionName=quCheckbox.getOptionName();
							if(quCheckbox.getIsNote() == 1){
								answerOtherText = anCheckbox.getOtherText();
							}
							break;
						}
					}
					answerOptionName=HtmlUtil.removeTagFromText(answerOptionName);
					answerOptionName = answerOptionName.replace(NBSP," ");
					exportUtil.setCell(cellIndex++, answerOptionName);
					if(quCheckbox.getIsNote() == 1)
						exportUtil.setCell(cellIndex++, answerOtherText);
				}
			} else if (quType == QuType.FILLBLANK) {// 填空题
				AnFillblank anFillblank=question.getAnFillblank();
				exportUtil.setCell(cellIndex++, anFillblank.getAnswer());
			} else if (quType == QuType.ANSWER) {// 多行填空题
				AnAnswer anAnswer=question.getAnAnswer();
				exportUtil.setCell(cellIndex++, anAnswer.getAnswer());
			} else if (quType == QuType.COMPRADIO) {// 复合单选题
				AnRadio anRadio=question.getAnRadio();
				String quItemId = anRadio.getQuItemId();
				List<QuRadio> quRadios=question.getQuRadios();
				String answerOptionName="";
				String answerOtherText="";
				for (QuRadio quRadio : quRadios) {
					String quRadioId=quRadio.getId();
					if(quRadioId.equals(quItemId)){
						answerOptionName=quRadio.getOptionName();
						answerOtherText=anRadio.getOtherText();
						break;
					}
				}
				answerOptionName=HtmlUtil.removeTagFromText(answerOptionName);
				answerOtherText=HtmlUtil.removeTagFromText(answerOtherText);
				answerOptionName = answerOptionName.replace(NBSP," ");
				exportUtil.setCell(cellIndex++, answerOptionName);
				exportUtil.setCell(cellIndex++, answerOtherText);
			} else if (quType == QuType.COMPCHECKBOX) {// 复合多选题
				List<AnCheckbox> anCheckboxs=question.getAnCheckboxs();
				List<QuCheckbox> checkboxs = question.getQuCheckboxs();
				for (QuCheckbox quCheckbox : checkboxs) {
					String quCkId=quCheckbox.getId();
					String answerOptionName="0";
					String answerOtherText="0";
					for (AnCheckbox anCheckbox : anCheckboxs) {
						String anQuItemId=anCheckbox.getQuItemId();
						if(quCkId.equals(anQuItemId)){
							answerOptionName=quCheckbox.getOptionName();
							answerOtherText=anCheckbox.getOtherText();
							break;
						}
					}
					answerOptionName=HtmlUtil.removeTagFromText(answerOptionName);
					answerOptionName = answerOptionName.replace(NBSP," ");
					exportUtil.setCell(cellIndex++, answerOptionName);
					if(1==quCheckbox.getIsNote()){
						answerOtherText=HtmlUtil.removeTagFromText(answerOtherText);
						exportUtil.setCell(cellIndex++, answerOtherText);
					}
				}
			} else if (quType == QuType.ENUMQU) {// 枚举题
				List<AnEnumqu> anEnumqus=question.getAnEnumqus();
				int enumNum = question.getParamInt01();
				for (int i = 0; i < enumNum; i++) {
					String answerEnum="";
					for (AnEnumqu anEnumqu : anEnumqus) {
						if(i==anEnumqu.getEnumItem()){
							answerEnum=anEnumqu.getAnswer();
							break;
						}
					}
					exportUtil.setCell(cellIndex++,  answerEnum);
				}
			} else if (quType == QuType.ORDERQU) {// 评分题
				List<QuOrderby> quOrderbys = question.getQuOrderbys();
				List<AnOrder> anOrders=question.getAnOrders();
				for (QuOrderby quOrderby : quOrderbys) {
					String quOrderbyId=quOrderby.getId();
					String answerOptionName="";
					for (AnOrder anOrder : anOrders) {
						if(quOrderbyId.equals(anOrder.getQuRowId())){
							answerOptionName=anOrder.getOrderyNum();
							break;
						}
					}
					answerOptionName = answerOptionName.replace(NBSP," ");
					exportUtil.setCell(cellIndex++, answerOptionName);
				}
			} else if (quType == QuType.MULTIFILLBLANK) {// 组合填空题
				List<QuMultiFillblank> quMultiFillblanks = question.getQuMultiFillblanks();
				List<AnDFillblank> anDFillblanks=question.getAnDFillblanks();
				for (QuMultiFillblank quMultiFillblank : quMultiFillblanks) {
					String quMultiFillbankId=quMultiFillblank.getId();
					String answerOptionName="";
					for (AnDFillblank anDFillblank : anDFillblanks) {
						if(quMultiFillbankId.equals(anDFillblank.getQuItemId())){
							answerOptionName=anDFillblank.getAnswer();
							break;
						}
					}
					answerOptionName = answerOptionName.replace(NBSP," ");
					exportUtil.setCell(cellIndex++, answerOptionName);
				}
			} else if (quType == QuType.SCORE) {// 评分题
				List<QuScore> quScores = question.getQuScores();
				List<AnScore> anScores=question.getAnScores();
				for (QuScore quScore : quScores) {
					String quScoreId=quScore.getId();
					String answerScore="";
					for (AnScore anScore : anScores) {
						if(quScoreId.equals(anScore.getQuRowId())){
							answerScore=anScore.getAnswserScore();
							break;
						}
					}
					exportUtil.setCell(cellIndex++, answerScore);
				}
			}else if (quType == QuType.UPLOADFILE) {
				//为导出文件
				List<AnUplodFile> anUplodFiles = question.getAnUplodFiles();
				StringBuilder answerBuf = new StringBuilder();

				String toFilePath = "";
				if(isExpUpQu){
					toFilePath = savePath + File.separator + orderNum + File.separator + "Q_" + quNum;
					File file = new File(toFilePath);
					if (!file.exists()) file.mkdirs();
				}
				for (AnUplodFile anUplodFile : anUplodFiles) {
					answerBuf.append(anUplodFile.getFileName()).append("      ");
					if(isExpUpQu){
						File fromFile = new File(DWSurveyConfig.DWSURVEY_WEB_FILE_PATH + anUplodFile.getFilePath());
						if (fromFile.exists()) {
							File toFile = new File(toFilePath + File.separator + anUplodFile.getFileName());
							try {
								FileUtil.copyFile(fromFile, toFile);
							} catch (IOException e) {
								logger.warning(e.getMessage());
							}
						}
					}
				}
				exportUtil.setCell(cellIndex++, answerBuf.toString());
			}
		}

		exportUtil.setCell(cellIndex++,  surveyAnswer.getIpAddr());
		exportUtil.setCell(cellIndex++,  surveyAnswer.getCity());
		exportUtil.setCell(cellIndex,  new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒").format(surveyAnswer.getEndAnDate()));


	}

	/**
	 * 以xls 导出问题标题
	 * @param exportUtil
	 * @param questions
	 */
	private void exportXLSTitle(XLSXExportUtil exportUtil,
								List<Question> questions) {
		exportUtil.createRow(0);
		int cellIndex = 0;


		int quNum=0;
		for (Question question : questions) {
			QuType quType = question.getQuType();
			if(quType==QuType.PAGETAG || quType==QuType.PARAGRAPH){
				continue;
			}
			quNum++;

			String quName = question.getQuTitle();
			quName=HtmlUtil.removeTagFromText(quName);
			String titleName =quNum +"、" + quName + "[" + quType.getCnName() + "]";
			if (quType == QuType.YESNO) {// 是非题
				exportUtil.setCell(cellIndex++, titleName);
			} else if (quType == QuType.RADIO) {// 单选题
				List<QuRadio> quRadios=question.getQuRadios();
				boolean isNote = false;
				for (QuRadio quRadio : quRadios) {
					if(quRadio.getIsNote()==1){
						isNote = true;
						break;
					}
				}
				exportUtil.setCell(cellIndex++, titleName);
				if(isNote) exportUtil.setCell(cellIndex++, titleName + "选项说明");

			} else if (quType == QuType.CHECKBOX) {// 多选题
				List<QuCheckbox> checkboxs = question.getQuCheckboxs();
				for (QuCheckbox quCheckbox : checkboxs) {
					String optionName = quCheckbox.getOptionName();
					optionName=HtmlUtil.removeTagFromText(optionName);
					exportUtil.setCell(cellIndex++,titleName + "－" + optionName );
					if(quCheckbox.getIsNote()==1){
						exportUtil.setCell(cellIndex++, titleName+ "－" + optionName  + "－选项说明");
					}
				}
			} else if (quType == QuType.FILLBLANK) {// 填空题
				exportUtil.setCell(cellIndex++, titleName);
			} else if (quType == QuType.ANSWER) {// 多行填空题
				exportUtil.setCell(cellIndex++, titleName);
			} else if (quType == QuType.COMPRADIO) {// 复合单选题
				exportUtil.setCell(cellIndex++, titleName);
				exportUtil.setCell(cellIndex++, titleName+"-说明" );

			} else if (quType == QuType.COMPCHECKBOX) {// 复合多选题
				List<QuCheckbox> checkboxs = question.getQuCheckboxs();
				for (QuCheckbox quCheckbox : checkboxs) {
					String optionName = quCheckbox.getOptionName();
					exportUtil.setCell(cellIndex++, titleName + "－"
							+ optionName);
					int isNote = quCheckbox.getIsNote();
					if (isNote == 1) {
						optionName=HtmlUtil.removeTagFromText(optionName);
						exportUtil.setCell(cellIndex++, titleName +"－"+ optionName
								+ "－" + "说明" );
					}
				}
			} else if (quType == QuType.ENUMQU) {// 枚举题
				int enumNum = question.getParamInt01();
				for (int i = 0; i < enumNum; i++) {
					exportUtil.setCell(cellIndex++,  titleName + i + "－枚举");
				}
			} else if (quType == QuType.MULTIFILLBLANK) {// 组合填空题
				List<QuMultiFillblank> quMultiFillblanks = question
						.getQuMultiFillblanks();
				for (QuMultiFillblank quMultiFillblank : quMultiFillblanks) {
					String optionName = quMultiFillblank.getOptionName();

					optionName=HtmlUtil.removeTagFromText(optionName);
					exportUtil.setCell(cellIndex++, titleName + "－"
							+ optionName);
				}
			} else if (quType == QuType.ORDERQU) {// 评分题
				List<QuOrderby> quOrderbys = question.getQuOrderbys();
				for (QuOrderby quOrderby : quOrderbys) {
					String optionName = quOrderby.getOptionName();
					optionName=HtmlUtil.removeTagFromText(optionName);
					exportUtil.setCell(cellIndex++, titleName+"_"+optionName);
				}
			} else if (quType == QuType.SCORE) {// 评分题
				List<QuScore> quScores = question.getQuScores();
				for (QuScore quScore : quScores) {
					String optionName = quScore.getOptionName();
					optionName=HtmlUtil.removeTagFromText(optionName);
					exportUtil.setCell(cellIndex++, titleName+"－"+optionName);
				}
			} else if (quType == QuType.UPLOADFILE) {
				exportUtil.setCell(cellIndex++, titleName);
			}
		}

		exportUtil.setCell(cellIndex++,  "回答者IP");
		exportUtil.setCell(cellIndex++,  "IP所在地");
		exportUtil.setCell(cellIndex,  "回答时间");

	}

	/**
	 * not implemented yet
	 */
	public void writeToXLS() {
		//not implement
	}


	/**
	 * 获取单个问卷的全局统计信息
	 * @param surveyStats
	 * @return
	 */
	@Override
	public SurveyStats surveyStatsData(SurveyStats surveyStats) {
		return surveyAnswerDao.surveyStatsData(surveyStats);
	}


	/**
	 * 取一份卷子回答的数据
	 */
	public Page<SurveyAnswer> answerPage(Page<SurveyAnswer> page,String surveyId){
		Criterion cri1=Restrictions.eq(SURVEYID, surveyId);
		Criterion cri2=Restrictions.lt("handleState", 2);
		page.setOrderBy(ENDANDATE);
		page.setOrderDir("desc");
		page=findPage(page, cri1, cri2);
		return page;
	}

	/**
	 * 根据用户ID获取该用户的所有答卷数据
	 *
	 * @param page 封装了答卷分页查询结果的对象
	 * @param userId 用户ID
	 * @return 封装了答卷分页查询结果的对象
	 */
	@Override
	public Page<SurveyAnswer> getAnswerPageByUserId(Page<SurveyAnswer> page, String userId) {
		Criterion criterion1 = Restrictions.eq(USER_ID, userId);
		Criterion criterion2 = Restrictions.lt("handleState", 2);
		page.setOrderBy(ENDANDATE);
		page.setOrderDir("desc");
		page = findPage(page, criterion1, criterion2);

		return page;
	}

	/**
	 * 返回答案列表
	 * @param surveyId
	 * @param isEff
	 * @return
	 */
	public List<SurveyAnswer> answerList(String surveyId,Integer isEff) {
		Criterion cri1=Restrictions.eq(SURVEYID, surveyId);
		Criterion cri2=Restrictions.lt("handleState", 2);
			Criterion cri3 = isEff!=null
					? Restrictions.eq("isEffective", isEff)
					: Restrictions.eq("isEffective", 1);
		return surveyAnswerDao.findByOrder(ENDANDATE,false, cri1, cri2, cri3);
	}


	/**
	 * 根据id更新回答数量
	 * @param surveyId
	 * @return
	 */
	@Transactional
	public SurveyDirectory upAnQuNum(String surveyId){
		SurveyDirectory survey = directoryManager.get(surveyId);
		surveyAnswerManagerImpl.upAnQuNum(survey);
		return survey;
	}

	/**
	 * 根据 surveyDirectory 对象更新回答数量
	 * @param survey
	 * @return
	 */
	@Transactional
	public SurveyDirectory upAnQuNum(SurveyDirectory survey) {
		Long answerCount = surveyAnswerDao.countResult(survey.getId());
		if(answerCount!=null){
			survey.setAnswerNum(answerCount.intValue());
			directoryManager.saveByAdmin(survey);
		}
		return survey;
	}

	/**
	 * 为一组 surveyDirectory 对象更新回答数量
	 * @param result
	 * @return
	 */
	@Override
	public List<SurveyDirectory> upAnQuNum(List<SurveyDirectory> result) {
		if(result!=null){
			for (SurveyDirectory survey:result) {
				surveyAnswerManagerImpl.upAnQuNum(survey);
			}
		}
		return result;
	}

	/**
	 * 删除指定id 的数据
	 * @param ids
	 */
	@Transactional
	@Override
	public void deleteData(String[] ids) {
		String surveyId = null;
		for (String id:ids) {
			SurveyAnswer surveyAnswer = get(id);
			surveyId = surveyAnswer.getSurveyId();
			surveyAnswerManagerImpl.delete(surveyAnswer);
		}
		if(surveyId!=null) surveyAnswerManagerImpl.upAnQuNum(surveyId);
	}

	/**
	 * 删除回答
	 * @param t
	 */
	@Override
	@Transactional
	public void delete(SurveyAnswer t) {
		if(t!=null){
			String belongAnswerId=t.getId();
			t.setHandleState(2);
			surveyAnswerDao.save(t);
			//更新当前答卷的回答记录值
			// 得到题列表
			List<Question> questions = questionManager.findDetails(t.getSurveyId(),"2");
			for (Question question : questions) {
				String quId=question.getId();
				QuType quType = question.getQuType();

				if (quType == QuType.YESNO) {// 是非题

				} else if (quType == QuType.RADIO) {// 单选题
					AnRadio anRadio=anRadioManager.findAnswer(belongAnswerId, quId);
					if(anRadio!=null){
						anRadio.setVisibility(0);
						//是否显示  1显示 0不显示
						anRadioManager.save(anRadio);
					}
				} else if (quType == QuType.CHECKBOX) {// 多选题
					List<AnCheckbox> anCheckboxs=anCheckboxManager.findAnswer(belongAnswerId, quId);
					if(anCheckboxs!=null){
						for (AnCheckbox anCheckbox : anCheckboxs) {
							anCheckbox.setVisibility(0);
							//是否显示  1显示 0不显示
							anCheckboxManager.save(anCheckbox);
						}
					}
				} else if (quType == QuType.FILLBLANK) {// 填空题
					AnFillblank anFillblank=anFillblankManager.findAnswer(belongAnswerId, quId);
					if(anFillblank!=null){
						anFillblank.setVisibility(0);
						//是否显示  1显示 0不显示
						anFillblankManager.save(anFillblank);
					}
				} else if (quType == QuType.ANSWER) {// 多行填空题

					AnAnswer anAnswer=anAnswerManager.findAnswer(belongAnswerId, quId);
					if(anAnswer!=null){
						anAnswer.setVisibility(0);
						//是否显示  1显示 0不显示
						anAnswerManager.save(anAnswer);
					}

				} else if (quType == QuType.COMPRADIO) {// 复合单选题

				} else if (quType == QuType.COMPCHECKBOX) {// 复合多选题

				} else if (quType == QuType.ENUMQU) {// 枚举题

				} else if (quType == QuType.MULTIFILLBLANK) {// 组合填空题
					List<AnDFillblank> anDFillblanks=anDFillblankManager.findAnswer(belongAnswerId, quId);
					if(anDFillblanks!=null){
						for (AnDFillblank anDFillblank : anDFillblanks) {
							anDFillblank.setVisibility(0);
							//是否显示  1显示 0不显示
							anDFillblankManager.save(anDFillblank);
						}
					}
				} else if (quType == QuType.SCORE) {// 评分题

					List<AnScore> anScores=anScoreManager.findAnswer(belongAnswerId, quId);
					if(anScores!=null){
						for (AnScore anScore : anScores) {
							anScore.setVisibility(0);
							//是否显示  1显示 0不显示
							anScoreManager.save(anScore);
						}

					}

				}

			}
		}
		super.delete(t);
	}

}
