#ifndef _COM_BONOVO_BLUETOOTH_CMD_H
#define _COM_BONOVO_BLUETOOTH_CMD_H

//ATָ��
enum {
	//2����Ӧ�ù淶ָ��
	CMD_AT_CA = 0,
	CMD_AT_CB,
	CMD_AT_CC,
	CMD_AT_CD,
	CMD_AT_CE,
	CMD_AT_CF,
	CMD_AT_CG,
	CMD_AT_CH,
	CMD_AT_CI,
	CMD_AT_CJ,
	CMD_AT_CK,
	CMD_AT_CL,
	CMD_AT_CM,
	CMD_AT_CO,
	CMD_AT_CW,
	CMD_AT_CX,
	CMD_AT_CY,
	CMD_AT_CN,
	CMD_AT_CP,
	//3������·��
	CMD_AT_WI,
	CMD_AT_MA,
	CMD_AT_MC,
	CMD_AT_MD,
	CMD_AT_ME,
	CMD_AT_MV,
	CMD_AT_MO,
	//4�绰��
	CMD_AT_PA,
	CMD_AT_PB,
	CMD_AT_PH,
	CMD_AT_PI,
	CMD_AT_PJ,
	CMD_AT_PF,
	CMD_AT_PE,
	CMD_AT_PG,
	CMD_AT_QA,
	CMD_AT_QB,
	CMD_AT_QC,
	//5�����ܲ���
	CMD_AT_CZ,
	CMD_AT_CV,
	CMD_AT_MY,
	CMD_AT_MG,
	CMD_AT_MH,
	CMD_AT_MP,
	CMD_AT_MQ,
	CMD_AT_MF,
	CMD_AT_MM,
	CMD_AT_MN,
	CMD_AT_MX,
	CMD_AT_DA,
	//added by leonkernan
	CMD_AT_CQ,
	CMD_AT_CR,
	CMD_AT_CS,
	CMD_AT_CT,
	CMD_AT_MZ,
	CMD_AT_QD,
	CMD_AT_QE,
	CMD_AT_PP,
	CMD_AT_MJ,
	CMD_AT_QJ,
	CMD_AT_QK,
	CMD_AT_MAX,
};

// codec status
typedef enum
{
	CODEC_LEVEL_NO_ANALOG = 0,
    CODEC_LEVEL_BT_MUSIC = 1,
    CODEC_LEVEL_AV_IN = 2,
    CODEC_LEVEL_DVB = 3,
    CODEC_LEVEL_DVD = 4,
    CODEC_LEVEL_RADIO = 5,
    CODEC_LEVEL_BT_TEL = 6,
    CODEC_LEVEL_COUNT
}CODEC_Level;

//#define DEBUG
#ifdef DEBUG
#define LOGV(fmt, args...)	ALOGV(fmt, ##args)
#define LOGD(fmt, args...)	ALOGD(fmt, ##args)
#define LOGI(fmt, args...)	ALOGI(fmt, ##args)
#define LOGW(fmt, args...)	ALOGW(fmt, ##args)
#define LOGE(fmt, args...)	ALOGE(fmt, ##args)
#else
#define LOGV(fmt, args...)
#define LOGD(fmt, args...)
#define LOGI(fmt, args...)
#define LOGW(fmt, args...)
#define LOGE(fmt, args...)
#endif

#endif
