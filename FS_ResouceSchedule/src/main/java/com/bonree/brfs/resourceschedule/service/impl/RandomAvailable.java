package com.bonree.brfs.resourceschedule.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bonree.brfs.common.utils.BrStringUtils;
import com.bonree.brfs.common.utils.Pair;
import com.bonree.brfs.resourceschedule.model.LimitServerResource;
import com.bonree.brfs.resourceschedule.model.ResourceModel;
import com.bonree.brfs.resourceschedule.service.AvailableServerInterface;

public class RandomAvailable implements AvailableServerInterface {
	private static final Logger LOG = LoggerFactory.getLogger("RandomAvailable");
	/**
	 * 存储资源信息
	 */
	private CopyOnWriteArrayList<ResourceModel> resource = new CopyOnWriteArrayList<ResourceModel>();
	private long updateTime = 0;
	private Map<Integer, String> snIds = new ConcurrentHashMap<>();
	private RandomAvailable(){
		
	}
	private static class simpleInstance{
		public static  RandomAvailable  instance = new RandomAvailable();
	}
	public static RandomAvailable getInstance(){
		return simpleInstance.instance;
	}
	/**
	 * scene场景 0：元数据操作，1：写入操作，2：读取操作
	 */
	@Override
	public String selectAvailableServer(int scene, String storageName) throws Exception {
		
		if(this.resource.isEmpty()){
			LOG.error("resource is empty");
			return null;
		}
		
		List<Pair<String, Double>> values = new ArrayList<Pair<String, Double>>();
		if(0 == scene){
			int index = Math.abs(new Random().nextInt())%this.resource.size();
			LOG.info("resource index : {}", index);
			return this.resource.get(index).getServerId();
		}
		if(BrStringUtils.isEmpty(storageName)){
			return null;
		}
		String server = null;
		double sum = 0.0;
		Pair<String, Double> tmp = null;
		for(ResourceModel ele : resource){
			server = ele.getServerId();
			if(1 == scene){
				sum = ele.getDiskRemainRate() + ele.getDiskWriteValue(storageName);
			}else if(2 == scene){
				sum = ele.getDiskReadValue(storageName);
			}else{
				continue;
			}
			tmp = new Pair<String, Double>(server,sum);
			values.add(tmp);
		}
		if(values.isEmpty()){
			LOG.error("values is empty");
			return null;
		}
		int index = getWeightRandom(values);
		
		return values.get(index).getFirst();
	}
	@Override
	public String selectAvailableServer(int scene, String storageName, List<String> exceptionServerList)
			throws Exception {
		if(this.resource.isEmpty()){
			return null;
		}
		List<ResourceModel> tmp = new ArrayList<ResourceModel>();
		if(exceptionServerList !=null && !exceptionServerList.isEmpty()){
			for(ResourceModel ele : this.resource){
				if(exceptionServerList.contains(ele.getServerId())){
					continue;
				}
				tmp.add(ele);
			}
			
		}else{
			tmp.addAll(this.resource);
		}
		if(tmp.isEmpty()){
			return null;
		}
		List<Pair<String, Double>> values = new ArrayList<Pair<String, Double>>();
		if(0 == scene){
			int index = Math.abs(new Random().nextInt())%tmp.size();
			return tmp.get(index).getServerId();
		}
		if(BrStringUtils.isEmpty(storageName)){
			return null;
		}
		String server = null;
		double sum = 0.0;
		Pair<String, Double> tmpResource = null;
		for(ResourceModel ele : tmp){
			server = ele.getServerId();
			if(exceptionServerList !=null && exceptionServerList.contains(server)){
				continue;
			}
			if(1 == scene){
				sum = ele.getDiskRemainRate() + ele.getDiskWriteValue(storageName);
			}else if(2 == scene){
				sum = ele.getDiskReadValue(storageName);
			}else{
				continue;
			}
			tmpResource = new Pair<String, Double>(server, sum);
			values.add(tmpResource);
		}
		if(values.isEmpty()){
			LOG.error("values is empty");
			return null;
		}
		int index = getWeightRandom(values);
		return values.get(index).getFirst();
	}
	@Override
	public List<Pair<String, Integer>> selectAvailableServers(int scene, String storageName) throws Exception {
		if(this.resource.isEmpty()){
			return null;
		}
		List<ResourceModel> tmp = new ArrayList<ResourceModel>();
		tmp.addAll(this.resource);
		if(tmp.isEmpty()){
			return null;
		}
		List<Pair<String, Double>> values = new ArrayList<Pair<String, Double>>();
		if(BrStringUtils.isEmpty(storageName)){
			return null;
		}
		String server = null;
		double sum = 0.0;
		Pair<String, Double> tmpResource = null;
		for(ResourceModel ele : tmp){
			server = ele.getServerId();
			if(1 == scene){
				sum = ele.getDiskRemainRate() + ele.getDiskWriteValue(storageName);
			}else if(2 == scene){
				sum = ele.getDiskReadValue(storageName);
			}else if(0 == scene){
				sum = ele.getNetTxValue() +ele.getNetRxValue();
			}else{
				continue;
			}
			tmpResource = new Pair<String, Double>(server,sum);
			values.add(tmpResource);
		}
		return converDoublesToIntegers(values).getSecond();
	}
	@Override
	public List<Pair<String, Integer>> selectAvailableServers(int scene, String storageName, List<String> exceptionServerList)
			throws Exception {
		if(this.resource.isEmpty()){
			return null;
		}
		List<ResourceModel> tmp = new ArrayList<ResourceModel>();
		if(exceptionServerList !=null && !exceptionServerList.isEmpty()){
			for(ResourceModel ele : this.resource){
				if(exceptionServerList.contains(ele.getServerId())){
					continue;
				}
				tmp.add(ele);
			}
			
		}else{
			tmp.addAll(this.resource);
		}
		if(tmp.isEmpty()){
			return null;
		}
		
		List<Pair<String, Double>> values = new ArrayList<Pair<String, Double>>();
		if(0 == scene){
			int index = Math.abs(new Random().nextInt())%tmp.size();
		}
		if(BrStringUtils.isEmpty(storageName)){
			return null;
		}
		String server = null;
		double sum = 0.0;
		Pair<String, Double> tmpResource = null;
		for(ResourceModel ele : tmp){
			server = ele.getServerId();
			if(1 == scene){
				sum = ele.getDiskRemainValue(storageName) + ele.getDiskWriteValue(storageName);
			}else if(2 == scene){
				sum = ele.getDiskReadValue(storageName);
			}else{
				continue;
			}
			tmpResource = new Pair<String, Double>(server,sum);
			values.add(tmpResource);
		}
		if(values == null || values.isEmpty()){
			return null;
		}
		return converDoublesToIntegers(values).getSecond();
	}
	@Override
	public void update(Collection<ResourceModel> resources) {
		if(resources == null || resources.isEmpty()){
			return;
		}
		this.resource.clear();
		this.resource.addAll(resources);
		this.updateTime = System.currentTimeMillis();
		Map<Integer,String> tmpMap = null;
		Map<Integer,String> sumMap = new ConcurrentHashMap<>();
		for(ResourceModel ele : resources){
			tmpMap = ele.getSnIds();
			if(tmpMap == null){
				continue;
			}
			sumMap.putAll(tmpMap);
		}
		this.snIds = sumMap;
	}
	@Override
	public long getLastUpdateTime() {
		return updateTime;
	}
	/**
	 * 概述：权重随机数
	 * @param servers
	 * @return
	 * @user <a href=mailto:zhucg@bonree.com>朱成岗</a>
	 */
	private int getWeightRandom(List<Pair<String, Double>> servers){
		Pair<Integer,List<Pair<String,Integer>>> resourceValues = converDoublesToIntegers(servers);

		int total = resourceValues.getFirst();
		List<Pair<String,Integer>> dents = resourceValues.getSecond();
		if(total == 0 || dents == null || dents.isEmpty()){
			return 0;
		}
		Random random = new Random();
		int randomNum = Math.abs(random.nextInt()%total);
		int current = 0;
		int index = 0;
		for(Pair<String, Integer> ele : dents){
			current += ele.getSecond();
			if(randomNum > current){
				index ++;
				continue;
			}
			if(randomNum <=current){
				break;
			}
		}
		return index;
	}
	/**
	 * 概述：计算资源比值
	 * @param servers
	 * @return
	 * @user <a href=mailto:zhucg@bonree.com>朱成岗</a>
	 */
	private Pair<Integer,List<Pair<String, Integer>>> converDoublesToIntegers(final List<Pair<String, Double>> servers){
		Pair<Integer, List<Pair<String, Integer>>> pair = new Pair<>();
		List<Pair<String,Integer>> dents = new ArrayList<Pair<String,Integer>>();
		int total = 0;
		int value = 0;
		double sum = 0;
		Pair<String,Integer> tmp = null;
		for(Pair<String,Double> ele : servers){
			sum += ele.getSecond();
		}
		for(Pair<String,Double> ele : servers){
			tmp = new Pair<String, Integer>();
			tmp.setFirst(ele.getFirst());
			value = (int)(ele.getSecond()/sum* 100);
			if(value == 0){
				value = 1;
			}
			tmp.setSecond(value);
			total += value;
			dents.add(tmp);
		}
		pair.setFirst(total);
		pair.setSecond(dents);
		return pair;
	}
	
	private Pair<String,Integer> converDoubleToInteger(final Pair<String, Double> source, int baseLine){
		Pair<String,Integer> dent = new Pair<String, Integer>();
		dent.setFirst(source.getFirst());
		dent.setSecond((int)(source.getSecond() * baseLine));
		return dent;
	}
	@Override
	public void setLimitParameter(LimitServerResource limits) {
				
	}
	@Override
	public String selectAvailableServer(int scene, int snId) throws Exception {
		String snName = this.snIds.get(snIds);
		return selectAvailableServer(scene, snName);
	}
	@Override
	public String selectAvailableServer(int scene, int snId, List<String> exceptionServerList) throws Exception {
		String snName = this.snIds.get(snIds);
		return selectAvailableServer(scene, snName, exceptionServerList);
	}
	@Override
	public List<Pair<String, Integer>> selectAvailableServers(int scene, int snId) throws Exception {
		String snName = this.snIds.get(snIds);
		
		return selectAvailableServers(scene, snName);
	}
	@Override
	public List<Pair<String, Integer>> selectAvailableServers(int scene, int snId, List<String> exceptionServerList)
			throws Exception {
		String snName = this.snIds.get(snIds);
		return selectAvailableServers(scene, snName,exceptionServerList);
	}
	
}
