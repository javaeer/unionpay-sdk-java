package cn.net.yunlou.unionpay;


import cn.net.yunlou.unionpay.sdk.CertUtil;
import cn.net.yunlou.unionpay.sdk.SDKConfig;
import cn.net.yunlou.unionpay.sdk.SDKConstants;
import cn.net.yunlou.unionpay.sdk.SDKUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 名称： 封装可用方法<br>
 * 日期： 2015-09<br>

 * 版权： 中国银联<br>
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
@Slf4j
public class UnionPayUtil {


	/**
	 * 获取当前时间，格式为 yyyyMMddHHmmss
	 *
	 * @return 当前时间字符串，格式为 yyyyMMddHHmmss
	 */
	public static String getCurrentTime() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	}

	/**
	 * 生成商户订单号，格式为 yyyyMMddHHmmssSSS
	 * 订单号长度为 AN8..40，不能包含 "-" 或 "_"
	 *
	 * @return 商户订单号字符串
	 */
	public static String getOrderId() {
		return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
	}

   /**
	 * 组装请求，返回报文字符串用于显示
	 * @param data 请求数据
	 * @return HTML格式的响应字符串
	 */
    public static String genHtmlResult(Map<String, String> data){

    	TreeMap<String, String> tree = new TreeMap<String, String>();
		Iterator<Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			tree.put(en.getKey(), en.getValue());
		}
		it = tree.entrySet().iterator();
		StringBuffer sf = new StringBuffer();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			String key = en.getKey();
			String value =  en.getValue();
			if("respCode".equals(key)){
				sf.append("<b>"+key + SDKConstants.EQUAL + value+"</br></b>");
			}else {
				sf.append(key + SDKConstants.EQUAL + value+"</br>");
			}
		}
		return sf.toString();
    }
    /**
	 * 功能：解析全渠道商户对账文件中的ZM文件并以List&lt;Map&gt;方式返回
	 * 适用交易：对账文件下载后对文件的查看
	 * @param filePath ZM文件全路径
	 * @return 包含每一笔交易中 序列号 和 值 的map序列
	 */
	public static List<Map> parseZMFile(String filePath){
		int lengthArray[] = {3,11,11,6,10,19,12,4,2,21,2,32,2,6,10,13,13,4,15,2,2,6,2,4,32,1,21,15,1,15,32,13,13,8,32,13,13,12,2,1,32,98};
		return parseFile(filePath,lengthArray);
	}

	/**
	 * 功能：解析全渠道商户对账文件中的ZME文件并以List&lt;Map&gt;方式返回
	 * 适用交易：对账文件下载后对文件的查看
	 * @param filePath ZME文件全路径
	 * @return 包含每一笔交易中 序列号 和 值 的map序列
	 */
	public static List<Map> parseZMEFile(String filePath){
		int lengthArray[] = {3,11,11,6,10,19,12,4,2,2,6,10,4,12,13,13,15,15,1,12,2,135};
		return parseFile(filePath,lengthArray);
	}

	/**
	 * 功能：解析全渠道商户 ZM,ZME对账文件
	 * @param filePath
	 * @param lengthArray 参照《全渠道平台接入接口规范 第3部分 文件接口》 全渠道商户对账文件 6.1 ZM文件和6.2 ZME 文件 格式的类型长度组成int型数组
	 * @return List&lt;Map&gt; 解析后的数据列表
	 */
	private static List<Map> parseFile(String filePath,int lengthArray[]){
	 	List<Map> ZmDataList = new ArrayList<Map>();
	 	try {
            String encoding="gbk"; //文件是gbk编码
            File file=new File(filePath);
            if(file.isFile() && file.exists()){ //判断文件是否存在
                InputStreamReader read = new InputStreamReader(
                new FileInputStream(file), "iso-8859-1");
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while((lineTxt = bufferedReader.readLine()) != null){
                	byte[] bs = lineTxt.getBytes("iso-8859-1");
                	//解析的结果MAP，key为对账文件列序号，value为解析的值
        		 	Map<Integer,String> ZmDataMap = new LinkedHashMap<Integer,String>();
                    //左侧游标
                    int leftIndex = 0;
                    //右侧游标
                    int rightIndex = 0;
                    for(int i=0;i<lengthArray.length;i++){
                    	rightIndex = leftIndex + lengthArray[i];
                    	String filed = new String(Arrays.copyOfRange(bs, leftIndex,rightIndex), encoding);
                    	leftIndex = rightIndex+1;
                    	ZmDataMap.put(i, filed);
                    }
                    ZmDataList.add(ZmDataMap);
                }
                read.close();
        }else{
            System.out.println("找不到指定的文件");
        }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }

		return ZmDataList;
	}

    /**
     * 将解析后的对账文件数据转换为 HTML 表格格式
     *
     * @param dataList 解析后的对账文件数据列表
     * @param file 文件名
     * @return HTML 格式的表格字符串
     */
    public static String getFileContentTable(List<Map> dataList,String file){
    	StringBuffer  tableSb = new StringBuffer("对账文件的规范参考 https://open.unionpay.com/ajweb/help/file/ 产品接口规范->平台接口规范:文件接口</br> 文件【"+file + "】解析后内容如下：");
    	tableSb.append("<table border=\"1\">");
    	if(dataList.size() > 0){
    		Map<Integer,String> dataMapTmp = dataList.get(0);
    		tableSb.append("<tr>");
	 		for(Iterator<Integer> it = dataMapTmp.keySet().iterator();it.hasNext();){
	 			Integer key = it.next();
	 			String value = dataMapTmp.get(key);
		 		System.out.println("序号："+ (key+1) + " 值: '"+ value +"'");
		 		tableSb.append("<td>序号"+(key+1)+"</td>");
		 	}
	 		tableSb.append("</tr>");
    	}

    	for(int i=0;i<dataList.size();i++){
	 		System.out.println("行数: "+ (i+1));
	 		Map<Integer,String> dataMapTmp = dataList.get(i);
	 		tableSb.append("<tr>");
	 		for(Iterator<Integer> it = dataMapTmp.keySet().iterator();it.hasNext();){
	 			Integer key = it.next();
	 			String value = dataMapTmp.get(key);
		 		System.out.println("序号："+ (key+1) + " 值: '"+ value +"'");
		 		tableSb.append("<td>"+value+"</td>");
		 	}
	 		tableSb.append("</tr>");
	 	}
    	tableSb.append("</table>");
    	return tableSb.toString();
    }


	/**
	 * 解压 ZIP 文件到指定目录
	 *
	 * @param zipFilePath ZIP 文件路径
	 * @param outPutDirectory 输出目录
	 * @return 解压后的文件路径列表
	 */
	public static List<String> unzip(String zipFilePath,String outPutDirectory){
		List<String> fileList = new ArrayList<String>();
		try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFilePath));//输入源zip路径
            BufferedInputStream bin = new BufferedInputStream(zin);
            BufferedOutputStream bout = null;
            File file=null;
            ZipEntry entry;
            try {
                while((entry = zin.getNextEntry())!=null && !entry.isDirectory()){
                	file = new File(outPutDirectory,entry.getName());
                    if(!file.exists()){
                        (new File(file.getParent())).mkdirs();
                    }
                    bout = new BufferedOutputStream(new FileOutputStream(file));
                    int b;
                    while((b=bin.read())!=-1){
                    	bout.write(b);
                    }
                    bout.flush();
                    fileList.add(file.getAbsolutePath());
                    System.out.println(file+"解压成功");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                try {
					bin.close();
					zin.close();
					if(bout!=null){
						bout.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
		return fileList;
	}




	/**
	 * 对控件支付成功返回的结果信息中data域进行验签（控件端获取的应答信息）<br>
	 * （目前环境有问题的样例）{  "sign" : "mtArQuB7XVMV/+CVqviSh7ntml5nbXpr8SDDiOQ+UjLPrAxzCE7r5huk63XCxej1ZMWNOIUdb0BLMovPlxZ6sS2Cifi8opF6Kcz8bhdi9Km7J69bLJNIRwRvNYM6SjHzn59hL6ZPiubas7/6nmz1QnESe7bdJAD5wCg6mB9oHdTncy83AWq8zvbYYIxGIzi5H0gHIsZbUdvMhljcxcnWLfdAdp5/BVEkSS7rdbUUOF6wu3RVrrKpI1C50Dcdx1yWGX1vOdrEJ6HWiZRpxb8ZtRE0/kCQFLaFHT9f8Hv4MLGOHB5tHPK2kWQIqidHJcXr3EPfH9Pu3YZ0MRH3vOVRkw==",  "data" : "pay_result=fail&tn=710065895678926661300&cert_id=69026276696"}
	 * （正常的测试环境样例）{"sign" : "J6rPLClQ64szrdXCOtV1ccOMzUmpiOKllp9cseBuRqJ71pBKPPkZ1FallzW18gyP7CvKh1RxfNNJ66AyXNMFJi1OSOsteAAFjF5GZp0Xsfm3LeHaN3j/N7p86k3B1GrSPvSnSw1LqnYuIBmebBkC1OD0Qi7qaYUJosyA1E8Ld8oGRZT5RR2gLGBoiAVraDiz9sci5zwQcLtmfpT5KFk/eTy4+W9SsC0M/2sVj43R9ePENlEvF8UpmZBqakyg5FO8+JMBz3kZ4fwnutI5pWPdYIWdVrloBpOa+N4pzhVRKD4eWJ0CoiD+joMS7+C0aPIEymYFLBNYQCjM0KV7N726LA==",  "data" : "pay_result=success&tn=201602141008032671528&cert_id=68759585097"}
	 * @return 是否成功
	 */
	private boolean validateAppResponse(String jsonData, String encoding) {

		if (SDKUtil.isEmpty(encoding)) {
			encoding = "UTF-8";
		}

		Pattern p = Pattern.compile("\\s*\"sign\"\\s*:\\s*\"([^\"]*)\"\\s*");
		Matcher m = p.matcher(jsonData);
		if(!m.find()) {
			log.error("内容不正确。");
			return false;
		}
		String sign = m.group(1);

		p = Pattern.compile("\\s*\"data\"\\s*:\\s*\"([^\"]*)\"\\s*");
		m = p.matcher(jsonData);
		if(!m.find()) {
			log.error("内容不正确。");
			return false;
		}
		String data = m.group(1);

		try {
			MessageDigest md = null;
			md = MessageDigest.getInstance("SHA-1");
			md.reset();
			md.update(data.getBytes(encoding));
			byte[] bs = md.digest();
			StringBuffer sb = new StringBuffer();
			for (byte b : bs) {
				String hex = Integer.toHexString(b & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
				sb.append(hex);
			}
			Signature st = Signature.getInstance("SHA1withRSA", "BC");
			PublicKey publicKey = getPublicKey(SDKConfig.getConfig().getModulus(), SDKConfig.getConfig().getExponent());
			st.initVerify(publicKey);
			st.update(sb.toString().toLowerCase().getBytes(encoding));
			return st.verify(Base64.decodeBase64(sign));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 从模数指数取公钥，建议改public static挪到个util类里。
	 * @param modulus 模数
	 * @param exponent 指数
	 * @return RSA公钥对象
	 */
	public static PublicKey getPublicKey(String modulus, String exponent) {
		try {
			CertUtil.getSignCertId(); //这句别删，要触发CertUtil加载BC，目前就这么凑合着用吧……

			BigInteger b1 = new BigInteger(modulus);
			BigInteger b2 = new BigInteger(exponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(b1, b2);
			return keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			log.error("公钥获取失败。");
			return null;
		}
	}

}