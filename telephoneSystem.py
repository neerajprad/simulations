class Estimator:
	"""Contains methods for computing point estimates and confidence intervals."""

	def __init__(self, coverage_pc):
		cdf = coverage_pc + (1 - coverage_pc)/2
		self.z_score = stats.norm.ppf(cdf)
		self.confStr = str(coverage_pc * 100) + "%"
		self.k = 0.0
		self.sum = 0.0
		self.v = 0.0

	def reset(self):
		"""Reset the Estimator to start simulation trials afresh."""
		self.k = 0.0
		self.sum = 0.0
		self.v = 0.0

	def processNextVal(self, value):
		self.k += 1
		if (self.k > 1):
			diff = self.sum - (self.k-1) * value 
			self.v += diff/self.k * diff/(self.k-1)
		self.sum += value

	def variance(self):
		return self.v/(self.k-1) if self.k > 1 else 0

	def mean(self):
		return self.sum/self.k if self.k > 0 else 0

	def numTrialsNeeded(self, eps, relative = True):
		width = self.mean() * eps if relative else eps
		print self.z_score, width
		return int(float(self.variance() * pow(self.z_score, 2))/pow(width, 2))

	def printConfInterval(self):
		pointEstimate = self.mean()
		halfwidth = self.z_score * pow(self.variance()/self.k, 0.5)
		clower, chigher = pointEstimate - halfwidth , pointEstimate + halfwidth
		print self.confStr, "Confidence Interval", "[",clower, ",", chigher, "]"


class randDist():
	"""Generation of random numbers belonging to a certain distribution"""
	def __init__(self, initSeed = None, **fn_params):
		self.distfn = fn_params.pop("distfn")
		self.rand = random.Random()	
		if initSeed:			
			self.rand.seed(initSeed)
		self.fnParams = fn_params		
		self.initstate = self.rand.getstate()

	def reset(self):
		self.rand.setstate(self.initstate)

	def getnext(self):
		return self.distfn(self, **self.fnParams)

	def uniform(self, u_min, u_max):
		return u_min + self.rand.random() * (u_max - u_min)

	def exponential(self, avg_lambda):
		return - (math.log(self.rand.random()))/avg_lambda



class priorityQueue:
	"""Priority queue using a heap array"""
	def __init__(self):
		self.pq = []
		self.entry_finder = {}

	def add_task(self, task, priority):
		if task in self.entry_finder:
			self.remove_task(task)
		newTask = [priority, task]
		self.entry_finder[task] = newTask
		heapq.heappush(self.pq, newTask)

	def remove_task(self, task):
		if task in self.entry_finder:
			toRemove = self.entry_finder.pop(task)
			toRemove[-1] = "<removed>"

	def next_task(self):
		if self.pq:
			nextTask = heapq.heappop(self.pq)
			if nextTask[-1] != "<removed>":
				return nextTask[-1], nextTask[0]
				self.entry_finder.pop(nextTask[-1])
			return self.next_task()
		else:
			raise KeyError('Popping from an empty priority queue')


class Clock:
	elapsedTime = 0.0
	def __init__(self, event, distn, pq):
		self.event = event
		self.distn = distn
		self.pq = pq
		self.active = False
		self.value = -1

	def set_clock(self, setValue = None):
		self.active = True
		if setValue is None:
			self.value = self.distn.getnext() + Clock.elapsedTime
		else:
			self.value = setValue + Clock.elapsedTime
		self.pq.add_task(self.event, self.value)

	def cancel_clock(self):
		self.pq.remove_task(self.event)
		self.active = False
		self.value = -1

	@staticmethod
	def next_event(pq):
		event, timeLeft = pq.next_task()
		Clock.elapsedTime += timeLeft
		return event, timeLeft

	def __str__(self):
		return self.event + ": " + str(self.value)

	__repr__ = __str__



class Params:
	clockDist = {}
	clockDist["callStart"] = {"distfn" : randDist.exponential, "avg_lambda" : 6}
	clockDist["callCompletion"] = {"distfn" : randDist.uniform, "u_min" : 0, "u_max" : 6}


class GSMP:
	"""Methods for accessing current state and inducing state transitions in the General State Markov Process model"""
	def __init__(self, N, K):
		self.numLines = N
		self.numLinks = K
		self.state = [0] * N
		self.eventClocks = {}
		self.pq = priorityQueue()
		self.availableLink = 1
		self.linkOccupancy = [None] * K
		self.initClocks([(N, "callStart", True), (K, "callCompletion", False)])

	def initClocks(self, instances):
		for numInstances, eventType, active in instances:
			for i in range(numInstances):
				clockdist = randDist(**Params.clockDist[eventType])
				eventName = eventType + "_" + str(i)
				self.eventClocks[eventName] = Clock(eventName, clockdist, self.pq)
				if active:
					self.eventClocks[eventName].set_clock()

	def trigger_event(self):
		event, time = Clock.next_event(self.pq)
		eventType, node = event.split("_")
		return eventType, int(node)

	def nextAvailableLink(self):
		for i in range(self.availableLink, self.numLinks):
			if not self.linkOccupancy[i]: return i
		return -1

	def transition(self):
		eventType, node = self.trigger_event()
		if eventType == "callStart":
			caller = node
			callee = random.randint(0, self.numLines - 1)
			if not self.state[callee] and self.availableLink != -1:
				self.eventClocking("callStart", caller, callee = callee, link = self.availableLink)
				self.state_change("callStart", caller, callee, self.availableLink)
			else:
				self.eventClocking("callLoss", caller)

		elif eventType == "callCompletion":
			caller, callee = self.linkOccupancy[node]
			self.eventClocking("callCompletion", caller, callee = callee, link = node)
			self.state_change("callCompletion", caller, callee, node)
			
	def state_change(self, eventType, caller, callee, link):
		if eventType == "callStart":
			self.state[caller] = link
			self.state[callee] = link
			self.linkOccupancy[link] = (caller, callee)
			self.availableLink = self.nextAvailableLink()

		elif eventType == "callCompletion":
			self.state[caller] = 0
			self.state[callee] = 0
			self.linkOccupancy[link] = None
			self.availableLink = min(link, self.availableLink)
			

	def eventClocking(self, eventType, caller, callee = None, link = None):
		if eventType == "callStart":
			self.eventClocks["callStart_" + str(caller)].cancel_clock()
			self.eventClocks["callStart_" + str(callee)].cancel_clock()
			self.eventClocks["callCompletion_" + str(link)].set_clock()
			
		elif eventType == "callCompletion":
			self.eventClocks["callStart_" + str(caller)].set_clock()
			self.eventClocks["callStart_" + str(callee)].set_clock()
			self.eventClocks["callCompletion_" + str(link)].cancel_clock()

		elif eventType == "callLoss":
			self.eventClocks["callStart_" + str(caller)].set_clock()



a = GSMP(4, 2)
while Clock.elapsedTime <= 100:
	a.transition()
	print Clock.elapsedTime




# pq = priorityQueue()
# clocks = []
# dist = [0]*10
# for i in range(5):
# 	dist[i] = randDist(distfn = randDist.uniform, u_min = 0, u_max = 6)
# 	clocks.append(Clock('event'+str(i), dist[i], pq))
# for i in range(5, 10):
# 	dist[i] = randDist(distfn = randDist.uniform, u_min = 0, u_max = 6)
# 	clocks.append(Clock('event'+str(i), dist[i], pq))

# for i in range(10):
# 	clocks[i].set_clock()


# clocks[9].cancel_clock()
# clocks[6].cancel_clock()

# event, timeLeft = Clock.next_event(pq)
# print Clock.elapsedTime
# print event, timeLeft

# print clocks
# print Clock.elapsedTime
# clocks[9].set_clock(setValue = 0.0)
# print Clock.elapsedTime, clocks[9].value - Clock.elapsedTime
# print clocks

# event, timeLeft = Clock.next_event(pq)
# print event, timeLeft

# clocks[6].set_clock()
# clocks[5].set_clock()
# print clocks

# event, timeLeft = Clock.next_event(pq)
# print event, timeLeft