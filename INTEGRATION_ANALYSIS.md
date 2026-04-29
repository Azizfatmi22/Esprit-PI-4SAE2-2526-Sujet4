# MS-Enrollment Microservice - Integration Analysis & Issues Resolution

## 📋 Summary of Issues & Fixes

### ✅ **ISSUE 1: LearnerId Type Mismatch in EmailService**
**Error:** "Erreur Wafa Cash : d != java.lang.String"
**Root Cause:** In `EmailService.buildPaymentSuccessEmail()`, the format specifier was `%d` (integer) instead of `%s` (string)
**Location:** Line 63 in `EmailService.java`
**Fix Applied:** Changed `%d` to `%s` to properly format String learnerId

```java
// BEFORE (WRONG):
<p style="margin: 8px 0 0; color: #6d28d9;"><strong>Apprenant ID:</strong> #%d</p>
// AFTER (FIXED):
<p style="margin: 8px 0 0; color: #6d28d9;"><strong>Apprenant ID:</strong> #%s</p>
```

**Status:** ✅ **FIXED**

---

## 🔗 **Microservices Integration Architecture**

### **Current Setup**
```
Frontend (Angular @ 4200)
    ↓
API Gateway (8085)
    ↓
    ├─ MS-Enrollment (Payment, Cart, Invoices)
    ├─ MS-Course (Course Catalog)
    ├─ Eureka Discovery (Service Registration)
    └─ Config Server
```

### **Data Flow During Payment**

```
1. CART PHASE
   Frontend → POST /msenrollment/cart/{learnerId}/add
   └─ Stores: courseId, courseTitle, coursePrice
   └─ Entity: Cart (learnerId: String, items: List<CartItem>)

2. PAYMENT INITIATION
   Frontend → POST /msenrollment/flouci/initiate/{learnerId}
   └─ Creates: FlouciTransaction (learnerId + transactionRef)
   └─ Sends OTP via SMS/Email

3. OTP VERIFICATION
   Frontend → POST /msenrollment/flouci/verify-otp
   └─ Validates OTP
   └─ Creates Payment (learnerId: String, amount, method)

4. ENROLLMENT CREATION
   Service → enrollmentService.createEnrollment(learnerId, courseId)
   └─ Creates: Enrollment (learnerId: String, courseId: Long)
   └─ Status: ACTIVE
   └─ Progress: 0.0%

5. INVOICE GENERATION
   Service → invoiceService.generateInvoice(learnerId, paymentId, ...)
   └─ Creates: Invoice (learnerId: String, paymentId, ...)

6. CART CLEARING
   Service → cartService.clearCart(learnerId)
   └─ Removes all items from cart

7. NOTIFICATION
   Service → notificationService.notifyPaymentSuccess(learnerId, ...)
   └─ Sends success email with invoice number
```

---

## 💾 **Database Schema: Learner ↔ Course Association**

### **Enrollment Table (Association Table)**
```sql
CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    learner_id VARCHAR(255) NOT NULL,        -- String UUID from User Service
    course_id BIGINT NOT NULL,               -- Foreign key to MS-Course
    status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    progress DOUBLE DEFAULT 0.0,
    enrolled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMP NULL,
    
    FOREIGN KEY (course_id) REFERENCES courses(id)  -- Cross-service reference
);
```

### **Cart Table**
```sql
CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    learner_id VARCHAR(255) NOT NULL UNIQUE, -- One cart per learner
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,               -- Foreign key to MS-Course
    course_title VARCHAR(255) NOT NULL,      -- Denormalized from MS-Course
    course_price DOUBLE NOT NULL,
    
    FOREIGN KEY (cart_id) REFERENCES cart(id)
);
```

### **Payments Table**
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    learner_id VARCHAR(255) NOT NULL,        -- String UUID
    amount DOUBLE NOT NULL,
    method ENUM('CARTE', 'FLOUCI', 'WAFA_CASH', 'BAKCHICH', 'PAYPAL'),
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_learner_id (learner_id)
);
```

---

## 🔍 **404 Error Investigation**

### **Error:** "Erreur lors de l'ajout au panier (404): Http failure response for http://localhost:8085/msenrollment/cart/be7f8f0b-bb1a-4590-a52a-a89b1a0e3b42/add: 404 Not Found"

### **Root Causes:**

1. **Invalid LearnerId Format**
   - The UUID `be7f8f0b-bb1a-4590-a52a-a89b1a0e3b42` is correct format
   - Ensure Frontend is sending the learnerId in the URL path correctly

2. **Missing Configuration in API Gateway**
   - Verify Gateway routes include: `/msenrollment/**` → MS-Enrollment
   - Check `application.properties` in API Gateway for correct service registration

3. **Service Not Registered in Eureka**
   - Verify MS-Enrollment is running and registered
   - Check logs for: `DiscoveryClient - registering service`

4. **CORS Issues**
   - If calling from Frontend directly without Gateway
   - Verify CORS configuration in MS-Enrollment

### **Solutions:**

#### **Option A: Verify Service Registration**
```bash
# Check Eureka Dashboard
http://localhost:8761

# Expected output should show:
# - API Gateway (port 8085)
# - MS-Enrollment (registered as msenrollment)
# - MS-Course (registered as mscourse)
```

#### **Option B: Test Cart Endpoint Directly**
```bash
# Direct call to MS-Enrollment (default port: 8888)
POST http://localhost:8888/msenrollment/cart/{learnerId}/add
Content-Type: application/json

{
  "courseId": 1,
  "courseTitle": "Angular Basics",
  "coursePrice": 49.99
}
```

#### **Option C: Verify Gateway Route Mapping**
File: `C:\Users\hazem\Desktop\aaaaaaaaaaaaaaaaaaaaa\backend\ApiGateway\src\main\resources\application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: msenrollment
          uri: lb://MSENROLLMENT           # Eureka service name (uppercase)
          predicates:
            - Path=/msenrollment/**
          filters:
            - StripPrefix=0
```

---

## 🚀 **Frontend Integration Checklist**

### **Step 1: Cart Management**
```typescript
// cart.service.ts
addToCart(learnerId: string, courseId: number, courseTitle: string, coursePrice: number) {
  return this.http.post(`/msenrollment/cart/${learnerId}/add`, {
    courseId: courseId,
    courseTitle: courseTitle,
    coursePrice: coursePrice
  });
}

getCart(learnerId: string) {
  return this.http.get(`/msenrollment/cart/${learnerId}`);
}
```

### **Step 2: Payment Processing (Flouci Example)**
```typescript
// payment.service.ts
initiateFlouciPayment(learnerId: string, phone: string, amount: number) {
  return this.http.post(`/msenrollment/flouci/initiate/${learnerId}`, {
    phoneNumber: phone,
    amount: amount
  });
}

verifyOtp(transactionRef: string, otp: string, learnerId: string, couponCode?: string) {
  return this.http.post(`/msenrollment/flouci/verify-otp`, {
    transactionRef: transactionRef,
    otp: otp,
    learnerId: learnerId,
    couponCode: couponCode || null
  });
}
```

### **Step 3: Enrollment Verification**
```typescript
// enrollment.service.ts
getUserEnrollments(learnerId: string) {
  return this.http.get(`/msenrollment/enrollments/learner/${learnerId}`);
}

// Should return list of Enrollments with:
// {
//   id: number,
//   learnerId: string (UUID),
//   courseId: number,
//   status: 'ACTIVE' | 'PENDING' | 'COMPLETED' | 'CANCELLED',
//   progress: number,
//   enrolledDate: date
// }
```

### **Step 4: Invoice & Email Verification**
```typescript
// After successful payment, verify:
// 1. Invoice email received (check mailbox)
// 2. Invoice number matches response
// 3. Learner ID is displayed correctly (as String UUID, not truncated)
```

---

## 🔌 **API Endpoints Reference**

### **Cart Endpoints**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/msenrollment/cart/{learnerId}` | Get learner's cart |
| POST | `/msenrollment/cart/{learnerId}/add` | Add course to cart |
| DELETE | `/msenrollment/cart/{learnerId}/remove/{itemId}` | Remove item from cart |

### **Payment Endpoints**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/msenrollment/flouci/initiate/{learnerId}` | Initiate Flouci payment |
| POST | `/msenrollment/flouci/verify-otp` | Verify OTP |
| POST | `/msenrollment/flouci/resend-otp` | Resend OTP |
| POST | `/msenrollment/wafa/pay/{learnerId}` | Process Wafa payment |
| POST | `/msenrollment/wafa/refund/{learnerId}` | Request Wafa refund |

### **Enrollment Endpoints**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/msenrollment/enrollments/learner/{learnerId}` | Get user enrollments |
| GET | `/msenrollment/enrollments/{enrollmentId}` | Get enrollment details |
| PUT | `/msenrollment/enrollments/{enrollmentId}/progress` | Update progress |

---

## ⚠️ **Common Pitfalls & Solutions**

### **Pitfall 1: LearnerId Type Inconsistency**
- ✅ **CORRECT:** All learnerId fields are `String` (UUID format)
- ❌ **WRONG:** Mixing Integer IDs with UUID strings
- **Solution:** Always convert learnerId from path variable as String

### **Pitfall 2: Cart Lifetime Management**
- Current behavior: Cart persists after payment
- **Recommendation:** Clear cart after successful payment (already implemented)
- **Add feature:** Auto-clear abandoned carts after 30 days

### **Pitfall 3: OTP Handling**
- OTP expires after 2 minutes
- OTP is case-sensitive (if numeric)
- Resend attempts reset the expiry timer

### **Pitfall 4: Cross-Service Communication**
- MS-Enrollment does NOT call MS-Course directly
- Course data is fetched by Frontend and passed to cart
- This maintains service independence

### **Pitfall 5: Email Delivery Issues**
- Emails go to: `inesjlassi245@gmail.com` (hardcoded)
- ⚠️ **TODO:** Replace with actual user email lookup service
- Check `EmailService.getEmailByLearnerId()` method

---

## 📊 **Deployment Checklist**

- [ ] Verify all services are registered in Eureka
- [ ] Test cart addition from Frontend (via Gateway port 8085)
- [ ] Test payment flow end-to-end
- [ ] Verify invoices are generated with correct UUID format
- [ ] Test email notifications are sent
- [ ] Verify enrollments are created in database
- [ ] Test OTP verification logic
- [ ] Test coupon application
- [ ] Test refund process (Wafa)

---

## 📝 **LearnerId Type Change Summary**

### Changed Entities:
- ✅ `Cart.learnerId: String`
- ✅ `CartItem` - no direct learnerId
- ✅ `Enrollment.learnerId: String`
- ✅ `Payment.learnerId: String`
- ✅ `FlouciTransaction.learnerId: String`
- ✅ `Invoice.learnerId: String`
- ✅ `WafaRefund.learnerId: String`
- ✅ `Notification.learnerId: String`

### Fixed Repositories:
- ✅ `CartRepository.findByLearnerId(String)`
- ✅ `EnrollmentRepository.findByLearnerId(String)`
- ✅ `PaymentRepository.findByLearnerId(String)`
- ✅ `FlouciTransactionRepository.findByLearnerId(String)`

### Fixed Format Specifiers:
- ✅ `EmailService.buildPaymentSuccessEmail()` - Changed `%d` to `%s`

---

## 🎯 **Next Steps for Frontend Integration**

1. **Update Angular Services:**
   - Ensure learnerId is obtained from authentication service (UUID format)
   - Pass as path parameter in all API calls

2. **Add Error Handling:**
   ```typescript
   .catch(error => {
     if (error.status === 404) {
       console.error('Service not found - Check Gateway routing');
     } else if (error.status === 400) {
       console.error('Bad request - Check learnerId format');
     }
   });
   ```

3. **Test Payment Flow:**
   - Cart: Add course
   - Payment: Initiate → Receive OTP → Verify OTP
   - Success: Get invoice number + email confirmation
   - Verify: Check enrollments in learner profile

4. **Monitor Logs:**
   - MS-Enrollment logs show payment processing
   - Check database for Enrollment records
   - Verify email service execution

---

## 🔐 **Security Notes**

- ✅ LearnerId is UUID (not sequential integer)
- ✅ All payment endpoints require learnerId validation
- ✅ OTP expires after 2 minutes
- ✅ Transactions are logged with learnerId + transactionRef
- ⚠️ Email hardcoded - needs user service integration
- ⚠️ No authentication check in current endpoints - add @Secured or JWT validation

---

**Last Updated:** March 3, 2026
**Status:** Integration Ready for Testing

